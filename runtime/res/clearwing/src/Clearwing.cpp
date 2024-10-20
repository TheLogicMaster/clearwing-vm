#include "Clearwing.h"

#include "java/lang/String.h"
#include "java/lang/Class.h"
#include "java/lang/Thread.h"
#include "java/lang/ClassCastException.h"
#include "java/lang/ArithmeticException.h"
#include "java/lang/InterruptedException.h"
#include "java/lang/NoSuchMethodError.h"
#include "java/lang/NullPointerException.h"
#include "java/lang/IllegalMonitorStateException.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IndexOutOfBoundsException.h"
#include "java/lang/OutOfMemoryError.h"
#include "java/lang/StackOverflowError.h"
#include "java/io/IOException.h"
#include "java/lang/Byte.h"
#include "java/lang/Short.h"
#include "java/lang/Character.h"
#include "java/lang/Integer.h"
#include "java/lang/Long.h"
#include "java/lang/Float.h"
#include "java/lang/Double.h"
#include "java/lang/Boolean.h"
#include "java/lang/Number.h"

#include <set>
#include <map>
#include <vector>
#include <mutex>
#include <iostream>
#include <cstring>
#include <locale>
#include <codecvt>
#include <atomic>
#include <unordered_set>
#include <cstdarg>

static_assert(sizeof(Class) == sizeof(java_lang_Class)); // Loosely ensure generated Class structure matches native representation
static_assert(std::alignment_of<java_lang_Object>() == std::alignment_of<jlong>()); // Embedding Object in type struct should not add padding

static std::vector<jobject> *objects;
static std::map<std::string, jclass> *classes;
static std::recursive_mutex criticalLock;
static std::mutex registryMutex;
static std::vector<jcontext> threadContexts;
static std::vector<jobject> deepMarkedObjects;

std::atomic_int64_t heapUsage;
std::atomic_int64_t allocationsSinceCollection;
int64_t lastCollectionHeapUsage;

extern "C" {

bool volatile suspendVM;

static void markPrimitive(jobject object, jint mark, jint depth) {}

static void clinitPrimitive(jcontext ctx) {}

// Todo: Extend `Type`
Class class_byte { .nativeName = (intptr_t)"B", .size = sizeof(jbyte), .staticInitializer = (intptr_t) clinitPrimitive, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_char { .nativeName = (intptr_t)"C", .size = sizeof(jchar), .staticInitializer = (intptr_t) clinitPrimitive, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_short { .nativeName = (intptr_t)"S", .size = sizeof(jshort), .staticInitializer = (intptr_t) clinitPrimitive, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_int { .nativeName = (intptr_t)"I", .size = sizeof(jint), .staticInitializer = (intptr_t) clinitPrimitive, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_long { .nativeName = (intptr_t)"J", .size = sizeof(jlong), .staticInitializer = (intptr_t) clinitPrimitive, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_float { .nativeName = (intptr_t)"F", .size = sizeof(jfloat), .staticInitializer = (intptr_t) clinitPrimitive, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_double { .nativeName = (intptr_t)"D", .size = sizeof(jdouble), .staticInitializer = (intptr_t) clinitPrimitive, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_boolean { .nativeName = (intptr_t)"Z", .size = sizeof(jbool), .staticInitializer = (intptr_t) clinitPrimitive, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_void { .nativeName = (intptr_t)"V", .size = 0, .staticInitializer = (intptr_t) clinitPrimitive, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };

void runVM(main_ptr entrypoint) {
    // Todo: Initialized flag to make this reusable
    registerClass(&class_byte);
    registerClass(&class_char);
    registerClass(&class_short);
    registerClass(&class_int);
    registerClass(&class_long);
    registerClass(&class_float);
    registerClass(&class_double);
    registerClass(&class_boolean);
    registerClass(&class_void);

    auto mainContext = createContext();
    auto thread = (jthread) gcAlloc(mainContext, &class_java_lang_Thread); // Todo: Ensure all fields are set, since constructor isn't called
    thread->parent.gcMark = GC_MARK_ETERNAL;
    thread->F_nativeContext = (intptr_t) mainContext;
    mainContext->thread = thread;
    thread->F_entrypoint = (intptr_t) entrypoint;
    thread->F_name = (intptr_t) stringFromNative(mainContext, "Main");
    threadEntrypoint(mainContext, thread);

    // Todo: Call System.exit() or otherwise terminate and join all threads when main finishes
}

/// Registers a class and populates its object fields. Does not throw exceptions.
bool registerClass(jclass clazz) {
    extern void *vtable_java_lang_Class[];

    registryMutex.lock();
    if (!classes)
        classes = new std::map<std::string, jclass>;
    (*classes)[(const char *) (intptr_t) clazz->nativeName] = clazz;
    clazz->parent = {
            .clazz = (intptr_t) &class_java_lang_Class,
            .gcMark = GC_MARK_ETERNAL,
            .vtable = (intptr_t) vtable_java_lang_Class,
            .monitor = (intptr_t) new ObjectMonitor,
    };
    clazz->instanceOfCache = (intptr_t) new std::set<jclass>();
    registryMutex.unlock();
    return true;
}

/// Retrieves a class or nullptr if one is not found. Does not throw exceptions.
jclass classForName(const char *name) {
    try {
        return classes->at(name); // This is thread-safe
    } catch (std::out_of_range &ex) {
        return nullptr;
    }
}

void instMultiANewArray(jcontext ctx, volatile jtype * volatile &sp, jclass type, int dimensionCount) {
    std::vector<int> dimensions;
    for (int i = 0; i < dimensionCount; i++)
        dimensions.push_back((int)(--sp)->i);
    std::reverse(dimensions.begin(), dimensions.end());
    (sp++)->o = (jobject) createMultiArray(ctx, type, dimensions);
}

jint floatCompare(jfloat value1, jfloat value2, jint nanValue) {
    return floatingCompare(value1, value2, nanValue);
}

jint doubleCompare(jdouble value1, jdouble value2, jint nanValue) {
    return floatingCompare(value1, value2, nanValue);
}

jint longCompare(jlong value1, jlong value2) {
    if (value1 > value2)
        return 1;
    else if (value1 < value2)
        return -1;
    else
        return 0;
}

static jstring createString(jcontext ctx, const char *string, int length) {
    auto encoded = std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>{}.from_bytes(string, string + length);
    int encodedLength = (int)encoded.length();
    auto inst = (jstring) gcAllocNative(ctx, &class_java_lang_String); // This leaks if createArray throws an exception
    inst->F_value = (intptr_t) createArray(ctx, &class_char, encodedLength);
    inst->parent.gcMark = GC_MARK_START;
    inst->F_count = encodedLength;
    memcpy(((jarray) inst->F_value)->data, encoded.c_str(), encoded.length() * 2);
    return inst;
}

/// Creates a string from a native string. Throws exceptions.
jstring stringFromNative(jcontext ctx, const char *string) {
    return createString(ctx, string, (int)strlen(string));
}

/// Creates a string from a native string. Throws exceptions.
jstring stringFromNativeLength(jcontext ctx, const char *string, int length) {
    return createString(ctx, string, length);
}

/// Creates a string from a StringLiteral. Throws exceptions.
jstring createStringLiteral(jcontext ctx, StringLiteral literal) {
    static std::map<const char *, jstring> pool;
    static std::mutex lock;
    jstring value{};
    lockGuard(ctx, lock, "createStringLiteral", [&]{
        auto it = pool.find(literal.string);
        if (it != pool.end()) {
            value = it->second;
            return;
        }
        value = createString(ctx, literal.string, literal.length);
        value->parent.gcMark = GC_MARK_ETERNAL;
        pool[literal.string] = value;
    });
    return value;
}

/// Returns a native string tied to the lifespan of the string object. Throws exceptions.
const char *stringToNative(jcontext ctx, jstring string) {
    if (!NULL_CHECK(string)->F_nativeString) { // Race condition here probably doesn't matter
        auto bytes = (jarray) M_java_lang_String_getBytes_R_Array1_byte(ctx, (jobject) string);
        string->F_nativeString = (intptr_t) new char[bytes->length + 1]{};
        memcpy((char *) string->F_nativeString, bytes->data, bytes->length);
    }
    return (const char *) string->F_nativeString;
}

jstring concatStringsRecipe(jcontext ctx, const char *recipe, int argCount, ...) {
    va_list args;
    va_start(args, argCount);
    volatile auto string = new std::string;
    jstring result{};
    tryFinally(ctx, "concatStringsRecipe", [&] {
        int term = 0;
        for (const char *it = recipe; *it; it++) {
            if (*it == 0x1 || *it == 0x2) {
                if (term++ >= argCount) throw std::runtime_error("Not enough args for string recipe");
                auto obj = va_arg(args, jobject);
                if (obj)
                    *string += stringToNative(ctx, (jstring)invokeVirtual<func_java_lang_Object_toString_R_java_lang_String, VTABLE_java_lang_Object_toString_R_java_lang_String>(ctx, obj));
                else
                    *string += "null";
            } else {
                *string += *it;
            }
        }
        result = stringFromNative(ctx, string->c_str());
    }, [&] {
        delete string;
        va_end(args); // This may be unsafe if an exception is thrown...
    });
    return result;
}

/// Returns whether a provided `type` is an instance of or inherits from `assignee`. Does not throw exceptions.
bool isAssignableFrom(jcontext ctx, jclass type, jclass assignee) {
    static std::mutex lock;

    if (assignee == type)
        return true;

    auto cache = (std::set<jclass> *) type->instanceOfCache;

    {
        std::lock_guard guard(lock);
        if (cache->contains(assignee))
            return true;
    }

    auto updateCache = [cache, type](){
        std::lock_guard guard(lock);
        cache->insert(type);
    };

    if (assignee->parentClass and isAssignableFrom(ctx, type, (jclass) assignee->parentClass)) {
        updateCache();
        return true;
    }

    if (type->arrayDimensions > 0 and assignee->arrayDimensions > 0 and isAssignableFrom(ctx, (jclass) type->componentClass, (jclass) assignee->componentClass)) {
        updateCache();
        return true;
    }

    auto interfaces = (jclass *) assignee->nativeInterfaces;
    for (int i = 0; i < assignee->interfaceCount; i++)
        if (isAssignableFrom(ctx, type, interfaces[i])) {
            updateCache();
            return true;
        }

    return false;
}

/// Checks whether an object is an instance or inherits from a given type. Does not throw exceptions.
bool isInstance(jcontext ctx, jobject object, jclass type) {
    if (!object)
        return false;
    return isAssignableFrom(ctx, type, (jclass) object->clazz);
}

/// Resolves an interface in an object vtable. Method index must be an index into the method metadata array of this exact interface (Not a super class). Throws exceptions.
void *resolveInterfaceMethod(jcontext ctx, jclass interface, int method, jobject object) {
    NULL_CHECK(object);
    static std::mutex lock;
    static std::map<jclass, std::map<jclass, std::vector<int>>> mappings;

    std::map<jclass, std::vector<int>> *objectMappings;
    {
        std::lock_guard guard(lock);
        auto mappingsIt = mappings.find(interface);
        if (mappingsIt == mappings.end())
            objectMappings = &mappings[interface];
        else
            objectMappings = &mappingsIt->second;
    }

    auto objectClass = (jclass) object->clazz;

    int offset;
    {
        std::lock_guard guard(lock);
        auto offsetsIt = objectMappings->find(objectClass);
        if (offsetsIt == objectMappings->end()) {
            std::vector<int> offsets(interface->methodCount);
            for (int i = 0; i < interface->methodCount; i++) {
                auto &metadata = ((MethodMetadata *) interface->nativeMethods)[i];
                if (metadata.access & 0x8) { // ACC_STATIC
                    offsets[i] = -1;
                    continue;
                }
                int found = -1;
                for (int j = 0; j < objectClass->vtableSize; j++) {
                    auto entry = ((VtableEntry *) objectClass->vtableEntries)[j];
                    if (strcmp(entry.name, metadata.name) != 0 or strcmp(entry.desc, metadata.desc) != 0)
                        continue;
                    found = j;
                    break;
                }
                offsets[i] = found;
            }
            (*objectMappings)[objectClass] = offsets;
            offset = offsets[method];
        } else {
            auto &offsets = offsetsIt->second;
            offset = offsets.size() <= method ? -1 : offsets[method];
        }
    }

    if (offset == -1)
        constructAndThrow<&class_java_lang_NoSuchMethodError, init_java_lang_NoSuchMethodError>(ctx);

    return ((void **) object->vtable)[offset];
}

/// Allocates an instance of a class. Caller must be at a safepoint, this calls the GC. Throws exceptions.
jobject gcAlloc(jcontext ctx, jclass clazz) {
    static std::mutex lock;

    if (!objects) {
        lock.lock();
        objects = new std::vector<jobject>;
        lock.unlock();
    }

    if (heapUsage > GC_HEAP_THRESHOLD || heapUsage - lastCollectionHeapUsage > GC_MEM_THRESHOLD || allocationsSinceCollection > GC_OBJECT_THRESHOLD)
        runGC(ctx);

    if (heapUsage > GC_HEAP_THRESHOLD)
        constructAndThrow<&class_java_lang_OutOfMemoryError, init_java_lang_OutOfMemoryError>(ctx);

    auto object = (jobject) new char[clazz->size]{};
    heapUsage += clazz->size + (int64_t) sizeof(ObjectMonitor);
    allocationsSinceCollection++;

    *object = {
            .clazz = (intptr_t) clazz,
            .gcMark = GC_MARK_START,
            .vtable = (intptr_t) clazz->classVtable,
            .monitor = (intptr_t) new ObjectMonitor,
    };

    lock.lock();
    objects->push_back(object);
    lock.unlock();

    return object;
}

/// Allocates an instance of a class and sets the GC mark to GC_MARK_NATIVE. Prefer storing objects on a stack frame. Throws exceptions.
jobject gcAllocNative(jcontext ctx, jclass clazz) {
    auto object = gcAlloc(ctx, clazz);
    object->gcMark = GC_MARK_NATIVE;
    return object;
}

/// Creates a new context. Does not throw exceptions.
jcontext createContext() {
    auto context = new Context;
    acquireCriticalLock();
    threadContexts.emplace_back(context);
    releaseCriticalLock();
    return context;
}

/// Destroys a context. Does not throw exceptions.
void destroyContext(jcontext context) {
    acquireCriticalLock();
    std::erase(threadContexts, context);
    releaseCriticalLock();
    delete context;
}

/// Runs the garbage collector
void runGC(jcontext ctx) {
    static std::atomic_bool running;
    static std::mutex suspendMutex;

    if (running.exchange(true))
        return;

    auto frameRef = pushStackFrame(ctx, 0, nullptr, "runGC", nullptr);

    // Suspend all threads before collecting (Suspended threads must have all owned objects reachable)
    {
        std::lock_guard lock(suspendMutex);
        suspendVM = true;
    }
    while (true) { // Todo: Signalling to make this less CPU intensive while waiting
        bool blocked = false;
        acquireCriticalLock();
        for (auto threadContext : threadContexts) {
            if (threadContext == ctx)
                continue;
            if (!threadContext->suspended && threadContext->thread->F_alive)
                blocked = true;
        }
        releaseCriticalLock();
        if (!blocked)
            break;
    }

    acquireCriticalLock();

    std::unordered_set<jobject> objectSet;
    objectSet.insert(objects->begin(), objects->end());

    static jint mark;
    if (++mark > GC_MARK_END)
        mark = GC_MARK_START + 1;

    deepMarkedObjects.clear();

    // Explicitly mark children of non-collectable objects
    for (auto object : *objects)
        if (object->gcMark > GC_MARK_COLLECTED && object->gcMark < GC_MARK_START)
            ((gc_mark_ptr) ((jclass) object->clazz)->markFunction)(object, mark, GC_DEPTH_ALWAYS);

    // Mark class objects (Not in `objects`)
    for (auto &pair : *classes)
        mark_java_lang_Class((jobject) pair.second, mark, GC_DEPTH_ALWAYS);

    // Mark static fields
    for (auto &pair : *classes)
        ((gc_mark_ptr) pair.second->markFunction)(nullptr, mark, GC_DEPTH_ALWAYS);

    // Mark stack objects
    for (auto threadContext : threadContexts) {
        for (int i = 0; i < threadContext->stackDepth; i++) {
            const auto &frame = threadContext->frames[i];
            for (int j = 0; j < frame.size; j++) {
                const auto obj = frame.frame[j].o;
                if (objectSet.contains(obj))
                    ((gc_mark_ptr) ((jclass) obj->clazz)->markFunction)(obj, mark, 0);
            }
        }
    }

    // Specially mark deep object chains to avoid stack overflows
    while (!deepMarkedObjects.empty()) {
        auto deep = deepMarkedObjects;
        deepMarkedObjects.clear();
        for (auto obj : deep)
            ((gc_mark_ptr) ((jclass) obj->clazz)->markFunction)(obj, mark, 0);
    }

    // Collect unmarked objects
    auto partitionIterator = std::partition(objects->begin(), objects->end(), [](jobject o){
        return (o->gcMark > GC_MARK_COLLECTED && o->gcMark < GC_MARK_START) || o->gcMark == mark; // `true` for reachable objects
    });
    std::vector<jobject> collected(partitionIterator, objects->end());
    objects->erase(partitionIterator, objects->end());

    allocationsSinceCollection = 0;

    releaseCriticalLock();
    {
        std::lock_guard lock(suspendMutex);
        suspendVM = false;
    }

    // Finalize collected objects
    for (auto obj : collected) {
        obj->gcMark = GC_MARK_COLLECTED;
        tryCatch(frameRef, [&]{
            ((finalizer_ptr)((void **)obj->vtable)[VTABLE_java_lang_Object_finalize])(ctx, obj);
        }, &class_java_lang_Throwable, [](jobject ignored){});
    }

    // Delete finalized objects
    for (auto obj : collected) {
        // Todo: "Object resurrection" is ignored in the unlikely event that a strong reference is made in a finalizer...
        // Todo: Maybe storing objects separately until the next collection before actually deleting them would work

        if (((jclass) obj->clazz)->arrayDimensions > 0)
            disposeArray(ctx, (jarray) obj);

        heapUsage -= ((jclass) obj->clazz)->size + (int64_t) sizeof(ObjectMonitor);

        delete (ObjectMonitor *) obj->monitor;

        memset(obj, 0, sizeof(java_lang_Object)); // Erase collected objects to make memory bugs easier to catch
        obj->gcMark = GC_MARK_COLLECTED; // Set collected flag again

        delete[] (char *) obj;
    }

    lastCollectionHeapUsage = heapUsage;

    running = false;

    popStackFrame(ctx);
}

void markDeepObject(jobject obj) {
    deepMarkedObjects.emplace_back(obj);
}

/// Adjust the heap usage stat by the given amount. Does not throw exceptions.
void adjustHeapUsage(int64_t amount) {
    heapUsage += amount;
}

// Acquires the global critical lock. Does not throw exceptions.
void acquireCriticalLock() {
    criticalLock.lock();
}

/// Releases the global critical lock. Does not throw exceptions.
void releaseCriticalLock() {
    criticalLock.unlock();
}

/// Suspends a thread while the VM is suspended. Does not throw exceptions.
void safepointSuspend(jcontext ctx) {
    ctx->suspended = true;
    while (suspendVM) {} // Todo: Don't busy-wait, use signalling
    ctx->suspended = false;
}

/// Pushes and returns a new stack frame. The `method` and `monitor` parameters are optional. Throws exceptions.
jframe pushStackFrame(jcontext ctx, int size, const jtype *stack, const char *method, jobject monitor) {
    SAFEPOINT(); // This is safe because parameters are the responsibility of the caller
    if (ctx->stackDepth + 1 >= MAX_STACK_DEPTH)
        constructAndThrow<&class_java_lang_StackOverflowError, init_java_lang_StackOverflowError>(ctx); // Todo: This causes a native stack overflow at preset...
    ctx->lock.lock();
    auto frame = &ctx->frames[ctx->stackDepth++];
    *frame = {size, stack, method, monitor};
    ctx->lock.unlock();
    if (frame->monitor)
        monitorEnter(ctx, frame->monitor);
    return frame;
}

/// Pops a stack frame. Does not throw exceptions.
void popStackFrame(jcontext ctx) {
    SAFEPOINT(); // Safepoint before popping the stack to preserve return value object references on the frame
    if (ctx->stackDepth == 0)
        throw std::runtime_error("No stack frame to pop");
    auto frame = &ctx->frames[ctx->stackDepth - 1];
    if (frame->monitor)
        monitorExit(ctx, frame->monitor);
    ctx->lock.lock();
    ctx->stackDepth -= 1;
    ctx->lock.unlock();
}

/// Pushes an exception frame onto the given stack frame. `type` can be null. Does not throw exceptions.
jmp_buf *pushExceptionFrame(jframe frame, jclass type) {
    return &frame->exceptionFrames.emplace_back<ExceptionFrame>({type}).landingPad;
}

/// Pops an exception frame then returns and clears the current exception. Does not throw exceptions.
jobject popExceptionFrame(jframe frame) {
    if (frame->exceptionFrames.empty()) 
        return nullptr; // Todo: Should this be possible to happen, or do jumps into a try-catch region need to be guarded? Seen in ktx-assets-async AssetStorage.load
    frame->exceptionFrames.pop_back();
    auto exception = frame->exception;
    frame->exception = nullptr;
    return exception;
}

/// Throws an exception. This function does not return.
void throwException(jcontext ctx, jobject exception) {
    while (ctx->stackDepth > 0) {
        auto &frame = ctx->frames[ctx->stackDepth - 1];
        while (!frame.exceptionFrames.empty()) {
            auto &exceptionFrame = frame.exceptionFrames.back();
            if (!exceptionFrame.type or isInstance(ctx, exception, exceptionFrame.type)) {
                frame.exception = exception;
                longjmp(exceptionFrame.landingPad, 1);
            }
            frame.exceptionFrames.pop_back();
        }
        if (frame.monitor)
            monitorExit(ctx, frame.monitor); // Todo: Stack overflows if this throws an exception
        ctx->lock.lock();
        ctx->stackDepth -= 1;
        ctx->lock.unlock();
    }
    // Todo: Include exception details
    throw std::runtime_error("Uncaught exception");
}

void throwDivisionByZero(jcontext ctx) {
    constructAndThrowMsg<&class_java_lang_ClassCastException, init_java_lang_ArithmeticException_java_lang_String>(ctx, "Division by Zero");
}

void throwClassCast(jcontext ctx) {
    constructAndThrow<&class_java_lang_ClassCastException, init_java_lang_ClassCastException>(ctx);
}

void throwNullPointer(jcontext ctx) {
    constructAndThrow<&class_java_lang_NullPointerException, init_java_lang_NullPointerException>(ctx);
}

void throwIndexOutOfBounds(jcontext ctx) {
    constructAndThrow<&class_java_lang_IndexOutOfBoundsException, init_java_lang_IndexOutOfBoundsException>(ctx);
}

void throwIllegalArgument(jcontext ctx) {
    constructAndThrow<&class_java_lang_IllegalArgumentException, init_java_lang_IllegalArgumentException>(ctx);
}

void throwIOException(jcontext ctx, const char *message) {
    if (message)
        constructAndThrowMsg<&class_java_io_IOException, init_java_io_IOException_java_lang_String>(ctx, message);
    else
        constructAndThrow<&class_java_io_IOException, init_java_io_IOException>(ctx);
}

/// Lock on a monitor. Throws exceptions.
void monitorEnter(jcontext ctx, jobject object) {
    NULL_CHECK(object);
    auto monitor = (jmonitor) object->monitor;

    // Race condition here is fine, fall back to slower/blocking acquire
    if (monitor->lock.try_lock()) {
        monitor->owner = ctx;
        monitor->depth++;
        return;
    }

    // Suspend thread to avoid deadlock blocking GC and deadlocking system
    ctx->blockedBy = object;
    ctx->suspended = true;

    monitor->lock.lock();
    monitor->owner = ctx;
    monitor->depth++;

    ctx->blockedBy = nullptr;
    ctx->suspended = false;

    SAFEPOINT();
}

/// Unlock on a monitor. Throws exceptions.
void monitorExit(jcontext ctx, jobject object) {
    NULL_CHECK(object);
    auto monitor = (jmonitor) object->monitor;
    monitorOwnerCheck(ctx, object);
    if (--monitor->depth == 0)
        monitor->owner = nullptr;
    monitor->lock.unlock();
}

/// Checks if the current thread owns a given monitor. Throws exceptions.
void monitorOwnerCheck(jcontext ctx, jobject object) {
    auto monitor = (jmonitor) object->monitor;
    if (!monitor->owner or monitor->owner != ctx)
        constructAndThrow<&class_java_lang_IllegalMonitorStateException, init_java_lang_IllegalMonitorStateException>(ctx);
}

/// Checks if the current thread is interrupted. Throws exceptions.
void interruptedCheck(jcontext ctx) {
    if (ctx->thread->F_interrupted) {
        ctx->thread->F_interrupted = false;
        constructAndThrow<&class_java_lang_InterruptedException, init_java_lang_InterruptedException>(ctx);
    }
}

/// Checks if an object is null. Throws exceptions.
jobject nullCheck(jcontext ctx, jobject object) {
    return NULL_CHECK(object);
}

jobject boxByte(jcontext ctx, jbyte value) {
    return SM_java_lang_Byte_valueOf_byte_R_java_lang_Byte(ctx, value);
}

jobject boxCharacter(jcontext ctx, jchar value) {
    return SM_java_lang_Character_valueOf_char_R_java_lang_Character(ctx, value);
}

jobject boxShort(jcontext ctx, jshort value) {
    return SM_java_lang_Short_valueOf_short_R_java_lang_Short(ctx, value);
}

jobject boxInteger(jcontext ctx, jint value) {
    return SM_java_lang_Integer_valueOf_int_R_java_lang_Integer(ctx, value);
}

jobject boxLong(jcontext ctx, jlong value) {
    return SM_java_lang_Long_valueOf_long_R_java_lang_Long(ctx, value);
}

jobject boxFloat(jcontext ctx, jfloat value) {
    return SM_java_lang_Float_valueOf_float_R_java_lang_Float(ctx, value);
}

jobject boxDouble(jcontext ctx, jdouble value) {
    return SM_java_lang_Double_valueOf_double_R_java_lang_Double(ctx, value);
}

jobject boxBoolean(jcontext ctx, jbool value) {
    return SM_java_lang_Boolean_valueOf_boolean_R_java_lang_Boolean(ctx, value);
}

jbyte unboxByte(jcontext ctx, jobject boxed) {
    if (NULL_CHECK(boxed)->clazz != (intptr_t) &class_java_lang_Byte)
        throwIllegalArgument(ctx);
    return ((java_lang_Byte *) boxed)->F_value;
}

jchar unboxCharacter(jcontext ctx, jobject boxed) {
    if (NULL_CHECK(boxed)->clazz != (intptr_t) &class_java_lang_Character)
        throwIllegalArgument(ctx);
    return ((java_lang_Character *) boxed)->F_value;
}

jshort unboxShort(jcontext ctx, jobject boxed) {
    if (NULL_CHECK(boxed)->clazz == (intptr_t) &class_java_lang_Byte)
        return unboxByte(ctx, boxed);
    if (boxed->clazz != (intptr_t) &class_java_lang_Short)
        throwIllegalArgument(ctx);
    return ((java_lang_Short *) boxed)->F_value;
}

jint unboxInteger(jcontext ctx, jobject boxed) {
    if (NULL_CHECK(boxed)->clazz == (intptr_t) &class_java_lang_Byte)
        return unboxByte(ctx, boxed);
    if (boxed->clazz == (intptr_t) &class_java_lang_Short)
        return unboxShort(ctx, boxed);
    if (boxed->clazz != (intptr_t) &class_java_lang_Integer)
        throwIllegalArgument(ctx);
    return ((java_lang_Integer *) boxed)->F_value;
}

jlong unboxLong(jcontext ctx, jobject boxed) {
    if (NULL_CHECK(boxed)->clazz == (intptr_t) &class_java_lang_Byte)
        return unboxByte(ctx, boxed);
    if (boxed->clazz == (intptr_t) &class_java_lang_Short)
        return unboxShort(ctx, boxed);
    if (boxed->clazz == (intptr_t) &class_java_lang_Integer)
        return unboxInteger(ctx, boxed);
    if (boxed->clazz != (intptr_t) &class_java_lang_Long)
        throwIllegalArgument(ctx);
    return ((java_lang_Long *) boxed)->F_value;
}

jfloat unboxFloat(jcontext ctx, jobject boxed) {
    if (NULL_CHECK(boxed)->clazz == (intptr_t) &class_java_lang_Byte)
        return unboxByte(ctx, boxed);
    if (boxed->clazz == (intptr_t) &class_java_lang_Short)
        return unboxShort(ctx, boxed);
    if (boxed->clazz == (intptr_t) &class_java_lang_Integer)
        return unboxInteger(ctx, boxed);
    if (boxed->clazz != (intptr_t) &class_java_lang_Float)
        throwIllegalArgument(ctx);
    return ((java_lang_Float *) boxed)->F_value;
}

jdouble unboxDouble(jcontext ctx, jobject boxed) {
    if (isInstance(ctx, boxed, &class_java_lang_Number))
        return invokeVirtual<func_java_lang_Number_doubleValue_R_double, VTABLE_java_lang_Number_doubleValue_R_double>(ctx, boxed);
    if (NULL_CHECK(boxed)->clazz != (intptr_t) &class_java_lang_Double)
        throwIllegalArgument(ctx);
    return ((java_lang_Double *) boxed)->F_value;
}

jbool unboxBoolean(jcontext ctx, jobject boxed) {
    if (NULL_CHECK(boxed)->clazz != (intptr_t) &class_java_lang_Boolean)
        throwIllegalArgument(ctx);
    return ((java_lang_Boolean *) boxed)->F_value;
}

}
