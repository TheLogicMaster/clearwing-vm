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

#include <set>
#include <map>
#include <vector>
#include <mutex>
#include <iostream>
#include <cstring>
#include <locale>
#include <codecvt>
#include <atomic>

static_assert(sizeof(Class) == sizeof(java_lang_Class)); // Loosely ensure generated Class structure matches native representation
static_assert(std::alignment_of<java_lang_Object>() == std::alignment_of<jlong>()); // Embedding Object in type struct should not add padding

static std::vector<jobject> *objects;
static std::map<std::string, jclass> *classes;
static std::recursive_mutex criticalLock;
static std::mutex registryMutex;
static Context mainContext;
static std::vector<jcontext> threadContexts{&mainContext};

std::atomic_int64_t heapUsage;

extern "C" {

bool volatile suspendVM;

static void markPrimitive(jobject object, jint mark) {}

// Todo: Extend `Type`
Class class_byte { .nativeName = (intptr_t)"B", .size = sizeof(jbyte), .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_char { .nativeName = (intptr_t)"C", .size = sizeof(jchar), .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_short { .nativeName = (intptr_t)"S", .size = sizeof(jshort), .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_int { .nativeName = (intptr_t)"I", .size = sizeof(jint), .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_long { .nativeName = (intptr_t)"J", .size = sizeof(jlong), .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_float { .nativeName = (intptr_t)"F", .size = sizeof(jfloat), .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_double { .nativeName = (intptr_t)"D", .size = sizeof(jdouble), .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_boolean { .nativeName = (intptr_t)"Z", .size = sizeof(jbool), .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };
Class class_void { .nativeName = (intptr_t)"V", .size = 0, .markFunction = (intptr_t) markPrimitive, .primitive = true, .access = 0x400 };

void runVM(main_ptr entrypoint) {
    registerClass(&class_byte);
    registerClass(&class_char);
    registerClass(&class_short);
    registerClass(&class_int);
    registerClass(&class_long);
    registerClass(&class_float);
    registerClass(&class_double);
    registerClass(&class_boolean);
    registerClass(&class_void);

    auto thread = (jthread) gcAlloc(&mainContext, &class_java_lang_Thread);
    // Todo: Call constructor or directly set fields like `name`
    thread->parent.gcMark = GC_MARK_ETERNAL;
    thread->F_nativeContext = (intptr_t) &mainContext;
    mainContext.thread = thread;
    thread->F_entrypoint = (intptr_t) entrypoint;
    threadEntrypoint(&mainContext, thread);

    // Todo: Call System.exit() or otherwise terminate and join all threads when main finishes
}

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
    auto inst = (jstring) gcAlloc(ctx, &class_java_lang_String);  // Safe because constructor isn't called
    inst->F_value = (intptr_t) createArray(ctx, &class_char, encodedLength);
    inst->F_count = encodedLength;
    memcpy(((jarray) inst->F_value)->data, encoded.c_str(), encoded.length() * 2);
    return inst;
}

jstring stringFromNative(jcontext ctx, const char *string) {
    return createString(ctx, string, (int)strlen(string));
}

jstring stringFromNativeLength(jcontext ctx, const char *string, int length) {
    return createString(ctx, string, length);
}

jstring createStringLiteral(jcontext ctx, StringLiteral literal) {
    static std::map<const char *, jstring> pool;
    static std::mutex mutex;
    mutex.lock();
    auto &entry = pool[literal.string];
    if (!entry) {
        entry = createString(ctx, literal.string, literal.length);
        entry->parent.gcMark = GC_MARK_ETERNAL;
    }
    mutex.unlock();
    return entry;
}

const char *stringToNative(jcontext ctx, jstring string) {
    if (!string->F_nativeString) { // Race condition here probably doesn't matter
        // Nothing is disposed on stack unwinding, so it's safe to call Java methods here
        auto bytes = (jarray) M_java_lang_String_getBytes_R_Array1_byte(ctx, (jobject) string); // Fine to use directly since String is final
        string->F_nativeString = (intptr_t) new char[bytes->length + 1]{};
        memcpy((char *) string->F_nativeString, bytes->data, bytes->length);
    }
    return (const char *) string->F_nativeString;
}

bool isAssignableFrom(jcontext ctx, jclass type, jclass assignee) {
    if (assignee == type)
        return true;

    auto cache = (std::set<jclass> *) type->instanceOfCache;
    if (cache->contains(assignee))
        return true;

    auto updateCache = [cache, type](){
        static std::mutex lock;
        lock.lock();
        cache->insert(type);
        lock.unlock();
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

bool isInstance(jcontext ctx, jobject object, jclass type) {
    if (!object)
        return false;
    return isAssignableFrom(ctx, type, (jclass) object->clazz);
}

// Method index must be an index into the method metadata array of this exactly interface (Not a super class)
void *resolveInterfaceMethod(jcontext ctx, jclass interface, int method, jobject object) {
    NULL_CHECK(object);
    static std::mutex lock;
    static std::map<jclass, std::map<jclass, std::vector<int>>> mappings;
    std::map<jclass, std::vector<int>> *objectMappings;
    try { // Todo: Use find instead
        objectMappings = &mappings.at(interface);
    } catch (std::out_of_range &ex) {
        lock.lock();
        objectMappings = &mappings[interface];
        lock.unlock();
    }
    auto objectClass = (jclass) object->clazz;
    int offset;
    try { // Todo: Use find instead
        auto &offsets = objectMappings->at(objectClass);
        offset = offsets.size() <= method ? -1 : offsets[method];
    } catch (std::out_of_range &ex) {
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
        lock.lock();
        (*objectMappings)[objectClass] = offsets;
        lock.unlock();
        offset = offsets[method];
    }
    if (offset == -1)
        constructAndThrow<&class_java_lang_NoSuchMethodError, init_java_lang_NoSuchMethodError>(ctx);
    return ((void **) object->vtable)[offset];
}

jobject gcAlloc(jcontext ctx, jclass clazz) {
    if (!objects)
        objects = new std::vector<jobject>;

    if (heapUsage > 1000000000) // Todo: This should incorporate objects allocated since last collection
        runGC(ctx);

//    if (heapUsage > 10000000)
//        constructAndThrow<&class_java_lang_OutOfMemoryError, init_java_lang_OutOfMemoryError>(ctx);

    auto object = (jobject) new char[clazz->size]{};
    adjustHeapUsage(clazz->size + (int64_t) sizeof(ObjectMonitor));
    objects->push_back(object); // Todo: This needs to be locked
    *object = {
            .clazz = (intptr_t) clazz,
            .gcMark = GC_MARK_START,
            .vtable = (intptr_t) clazz->classVtable,
            .monitor = (intptr_t) new ObjectMonitor,
    };

    return object;
}

jobject gcAllocNative(jcontext ctx, jclass clazz) {
    auto object = gcAlloc(ctx, clazz);
    object->gcMark = GC_MARK_NATIVE;
    return object;
}

void runGC(jcontext ctx) {
    static std::atomic_bool running;
    static std::mutex suspendMutex;

    if (running)
        return;
    running = true;

    auto frameRef = pushStackFrame(ctx, 0, nullptr, "runGC", nullptr);

    // Suspend all threads before collecting (Suspended threads must have all owned objects reachable)
    {
        std::lock_guard lock(suspendMutex); // Todo: Is the mutex sufficient in terms of memory barriers?
        suspendVM = true;
    }
    while (true) { // Todo: Signalling to make this less CPU intensive while waiting
        bool blocked = false;
        acquireCriticalLock();
        for (auto threadContext : threadContexts) {
            if (threadContext == ctx)
                continue;
            if (!threadContext->suspended)
                blocked = true;
        }
        releaseCriticalLock();
        if (!blocked)
            break;
    }

    acquireCriticalLock();

    // Allows using std::lower_bound instead of std::find for lookups while maintaining O(1) insertions for normal execution
    std::sort(objects->begin(), objects->end());

    static jint mark;
    if (++mark > GC_MARK_END)
        mark = 1;

    // Explicitly mark children of non-collectable objects
    for (auto object : *objects)
        if (object->gcMark > GC_MARK_COLLECTED && object->gcMark < GC_MARK_START)
            ((gc_mark_ptr)((jclass)object->clazz)->markFunction)(object, mark);

    // Mark class objects (Not in `objects`)
    for (auto &pair : *classes)
        mark_java_lang_Class((jobject) pair.second, mark);

    // Mark static fields
    for (auto &pair : *classes)
        ((gc_mark_ptr)pair.second->markFunction)(nullptr, mark);

    // Mark stack objects
    for (auto threadContext : threadContexts)
        for (const auto &frame : threadContext->frames)
            for (int j = 0; j < frame.size; j++) {
                const auto &obj = frame.frame[j].o;
                auto found = std::lower_bound(objects->begin(), objects->end(), obj);
//                auto found = std::find(objects->begin(), objects->end(), obj);
                if (*found == obj)
                    ((gc_mark_ptr)((jclass)obj->clazz)->markFunction)(obj, mark);
            }

    // Collect unmarked objects
    auto partitionIterator = std::partition(objects->begin(), objects->end(), [](jobject o){
        return (o->gcMark > GC_MARK_COLLECTED && o->gcMark < GC_MARK_START) || o->gcMark == mark; // `true` for reachable objects
    });
    std::vector<jobject> collected(partitionIterator, objects->end());
    objects->erase(partitionIterator, objects->end());

    releaseCriticalLock();
    {
        std::lock_guard lock(suspendMutex);
        suspendVM = false;
    }

    // Finalize collected objects
    for (auto obj : collected) {
        obj->gcMark = GC_MARK_COLLECTED;
//        std::cout << "Finalizing: " << obj << " " << (const char *)((jclass)obj->clazz)->nativeName << std::endl;
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

        adjustHeapUsage(-((jclass) obj->clazz)->size - (int64_t) sizeof(ObjectMonitor));

        delete (ObjectMonitor *) obj->monitor;
        memset(obj, 0, sizeof(java_lang_Object)); // Todo: Temp
        delete[] (char *) obj;
    }

    running = false;

    popStackFrame(ctx);
}

void adjustHeapUsage(int64_t amount) {
    static std::mutex lock;
    lock.lock();
    heapUsage += amount;
    lock.unlock();
}

void acquireCriticalLock() {
    criticalLock.lock();
}

void releaseCriticalLock() {
    criticalLock.unlock();
}

void safepointSuspend(jcontext ctx) {
    ctx->suspended = true;
    while (suspendVM) {} // Todo: Don't busy-wait, use signalling
    ctx->suspended = false;
}

jframe pushStackFrame(jcontext ctx, int size, const jtype *stack, const char *method, jobject monitor) {
    SAFEPOINT(); // This is safe because parameters are the responsibility of the caller
    if (ctx->stackDepth >= MAX_STACK_DEPTH)
        constructAndThrow<&class_java_lang_StackOverflowError, init_java_lang_StackOverflowError>(ctx);
    ctx->lock.lock();
    auto frame = &ctx->frames[ctx->stackDepth++];
    *frame = {size, stack, method, monitor};
    ctx->lock.unlock();
    return frame;
}

void popStackFrame(jcontext ctx) {
    SAFEPOINT(); // Safepoint before popping the stack to preserve return value object references on the frame
    ctx->lock.lock();
    ctx->stackDepth -= 1;
    ctx->lock.unlock();
}

jmp_buf *pushExceptionFrame(jframe frame, jclass type) {
    return &frame->exceptionFrames.emplace_back<ExceptionFrame>({type}).landingPad;
}

jobject popExceptionFrame(jframe frame) {
    frame->exceptionFrames.pop_back();
    auto exception = frame->exception;
    frame->exception = nullptr;
    return exception;
}

void throwException(jcontext ctx, jobject exception) {
    while (ctx->stackDepth > 0) {
        auto &frame = ctx->frames[ctx->stackDepth - 1];
        while (!frame.exceptionFrames.empty()) {
            auto &exceptionFrame = frame.exceptionFrames.back();
            if (isInstance(ctx, exception, exceptionFrame.type)) {
                frame.exception = exception;
                longjmp(exceptionFrame.landingPad, 1);
            }
            frame.exceptionFrames.pop_back();
        }
        if (frame.monitor)
            monitorExit(ctx, frame.monitor);
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
    constructAndThrow<&class_java_lang_ClassCastException, &init_java_lang_ClassCastException>(ctx);
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

void monitorEnter(jcontext ctx, jobject object) {
    NULL_CHECK(object);
    auto monitor = (jmonitor) object->monitor;

    // Race condition here is fine, fall back to slower/blocking acquire
    if (monitor->lock.try_lock()) {
        monitor->owner = ctx;
        return;
    }

    // Suspend thread to avoid deadlock blocking GC and deadlocking system
    ctx->lock.lock();
    ctx->blockedBy = object;
    ctx->lock.unlock();
    ctx->suspended = true;

    monitor->lock.lock();
    monitor->owner = ctx;

    ctx->lock.lock();
    ctx->blockedBy = nullptr;
    ctx->lock.unlock();
    ctx->suspended = false;
    SAFEPOINT();
}

void monitorExit(jcontext ctx, jobject object) {
    NULL_CHECK(object);
    auto monitor = (jmonitor) object->monitor;
    monitorOwnerCheck(ctx, object);
    monitor->owner = nullptr;
    monitor->lock.unlock();
}

void monitorOwnerCheck(jcontext ctx, jobject object) {
    auto monitor = (jmonitor) object->monitor;
    if (!monitor->owner or monitor->owner != ctx)
        constructAndThrow<&class_java_lang_IllegalMonitorStateException, init_java_lang_IllegalMonitorStateException>(ctx);
}

void interruptedCheck(jcontext ctx) {
    if (ctx->thread->F_interrupted) { // Race condition here doesn't matter, probably
        ctx->thread->F_interrupted = false;
        constructAndThrow<&class_java_lang_InterruptedException, init_java_lang_InterruptedException>(ctx);
    }
}

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

// Todo: These unbox functions need to have a context to do null (and maybe type) checking

jbyte unboxByte(jobject boxed) {
    return ((java_lang_Byte *) boxed)->F_value;
}

jchar unboxCharacter(jobject boxed) {
    return ((java_lang_Character *) boxed)->F_value;
}

jshort unboxShort(jobject boxed) {
    return ((java_lang_Short *) boxed)->F_value;
}

jint unboxInteger(jobject boxed) {
    return ((java_lang_Integer *) boxed)->F_value;
}

jlong unboxLong(jobject boxed) {
    return ((java_lang_Long *) boxed)->F_value;
}

jfloat unboxFloat(jobject boxed) {
    return ((java_lang_Float *) boxed)->F_value;
}

jdouble unboxDouble(jobject boxed) {
    return ((java_lang_Double *) boxed)->F_value;
}

jbool unboxBoolean(jobject boxed) {
    return ((java_lang_Boolean *) boxed)->F_value;
}

}
