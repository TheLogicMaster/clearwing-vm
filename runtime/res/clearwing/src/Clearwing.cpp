#include "Clearwing.h"

#include "java/lang/String.h"
#include "java/lang/Class.h"
#include "java/lang/Thread.h"
#include "java/lang/ClassCastException.h"
#include "java/lang/ArithmeticException.h"
#include "java/lang/InterruptedException.h"
#include "java/lang/NoSuchMethodError.h"
#include "java/lang/RuntimeException.h"
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
#include <java/nio/ByteBuffer.h>

#include <ankerl/unordered_dense.h>

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
#include <ranges>
#include <chrono>
#include <thread>

static_assert(sizeof(Class) == sizeof(java_lang_Class)); // Loosely ensure generated Class structure matches native representation
static_assert(std::alignment_of<java_lang_Object>() == std::alignment_of<jlong>()); // Embedding Object in type struct should not add padding

static ankerl::unordered_dense::set<jobject> *objects;
static ankerl::unordered_dense::set<jobject> *rootObjects;
static std::vector<jobject> collectedObjects;
static std::mutex objectsLock;
static jthread collectionThread;
static std::map<std::string, jclass> *classes;
static std::recursive_mutex criticalLock;
static std::mutex *registryMutex;
static std::vector<jcontext> threadContexts;
static std::vector<jobject> deepMarkedObjects;
static volatile bool exiting;

std::atomic_int64_t heapUsage;
std::atomic_int64_t allocationsSinceCollection;
int64_t lastCollectionHeapUsage;

extern "C" {

bool volatile suspendVM;

static void collectionThreadFunc(jcontext ctx);

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

    jcontext collectionCtx = createContext();
    collectionThread = (jthread) gcAllocEternal(mainContext, &class_java_lang_Thread); // Todo: Ensure all fields are set, since constructor isn't called
    collectionThread->F_nativeContext = (intptr_t) collectionCtx;
    collectionCtx->thread = collectionThread;
    collectionThread->F_started = true;
    collectionThread->F_name = (intptr_t) stringFromNative(mainContext, "GC");
    collectionCtx->nativeThread = new std::thread;
    *collectionCtx->nativeThread = std::thread(collectionThreadFunc, collectionCtx);

    auto thread = (jthread) gcAllocEternal(mainContext, &class_java_lang_Thread); // Todo: Ensure all fields are set, since constructor isn't called
    thread->F_nativeContext = (intptr_t) mainContext;
    mainContext->thread = thread;
    thread->F_entrypoint = (intptr_t) entrypoint;
    thread->F_name = (intptr_t) stringFromNative(mainContext, "Main");
    threadEntrypoint(mainContext, thread);

    exitVM(mainContext, 0);
}

void exitVM(jcontext ctx, int result) {
    if (exiting) return;
    exiting = true;
    ctx->dead = true;
    ctx->suspended = true;
    auto timeout = std::chrono::system_clock::now() + std::chrono::seconds(10);
    while (std::chrono::system_clock::now() < timeout) {
        suspendVM = true;
        bool done = true;
        for (auto threadContext : threadContexts) { // Todo: Should be locked for
            if (threadContext->dead) continue;
            done = false;
            threadContext->lock.lock();
            if (threadContext->blockedBy)
                ((jmonitor) ((jobject) threadContext->blockedBy)->monitor)->condition.notify_all();
            threadContext->lock.unlock();
        }
        if (done)
            break;
    }
    exit(result);
}

/// Registers a class and populates its object fields. Does not throw exceptions.
bool registerClass(jclass clazz) {
    extern void *vtable_java_lang_Class[];

    if (!registryMutex)
        registryMutex = new std::mutex;
    registryMutex->lock();
    if (!classes)
        classes = new std::map<std::string, jclass>;
    (*classes)[(const char *) (intptr_t) clazz->nativeName] = clazz;
    clazz->parent = {
            .clazz = (intptr_t) &class_java_lang_Class,
            .gcMark = GC_MARK_ETERNAL,
            .vtable = (intptr_t) vtable_java_lang_Class,
            .monitor = (intptr_t) new ObjectMonitor,
    };
    auto instanceCache = new std::set<jclass>;
    std::function<void(jclass)> processClass;
    processClass = [&](jclass cls) {
        instanceCache->emplace(cls);
        if (cls->parentClass)
            processClass((jclass)cls->parentClass);
        for (int i = 0; i < cls->interfaceCount; i++)
            processClass(((jclass *)cls->nativeInterfaces)[i]);
    };
    processClass(clazz);
    clazz->instanceOfCache = (intptr_t) instanceCache;
    registryMutex->unlock();
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

void instMultiANewArray(jcontext ctx, volatile jtype *&sp, jclass type, int dimensionCount) {
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
    if (value1 < value2)
        return -1;
    return 0;
}

static jstring createString(jcontext ctx, const char *string, int length, bool protect) {
    auto encoded = std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>{}.from_bytes(string, string + length);
    int encodedLength = (int)encoded.length();
    auto inst = (jstring) gcAllocProtected(ctx, &class_java_lang_String); // This leaks if createArray throws an exception
    inst->F_value = (intptr_t) createArray(ctx, &class_char, encodedLength);
    if (!protect)
        unprotectObject((jobject)inst);
    inst->F_count = encodedLength;
    memcpy(((jarray) inst->F_value)->data, encoded.c_str(), encoded.length() * 2);
    return inst;
}

/// Creates a string from a native string. Throws exceptions.
jstring stringFromNative(jcontext ctx, const char *string) {
    return createString(ctx, string, (int)strlen(string), false);
}

/// Creates a string from a native string. Throws exceptions.
jstring stringFromNativeLength(jcontext ctx, const char *string, int length) {
    return createString(ctx, string, length, false);
}

jstring stringFromNativeProtected(jcontext ctx, const char *string) {
    return createString(ctx, string, (int)strlen(string), true);
}

jstring stringFromNativeEternal(jcontext ctx, const char *string) {
    auto str = createString(ctx, string, (int)strlen(string), false);
    makeEternal((jobject)str);
    return str;
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
        value = createString(ctx, literal.string, literal.length, false);
        makeEternal((jobject)value);
        makeEternal((jobject)value->F_value);
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

/// Returns whether a provided `assignee` is an instance of or inherits from `type`. Does not throw exceptions.
bool isAssignableFrom(jcontext ctx, jclass type, jclass assignee) {
    if (type == assignee || type == &class_java_lang_Object)
        return true;

    if (type->arrayDimensions > 0 and assignee->arrayDimensions > 0)
        return isAssignableFrom(ctx, (jclass) type->componentClass, (jclass) assignee->componentClass);

    return ((std::set<jclass> *)assignee->instanceOfCache)->contains(type);
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

    auto objectClass = (jclass) object->clazz;

    int offset;
    {
        std::lock_guard guard(lock);

        std::map<jclass, std::vector<int>> &objectMappings = mappings[interface];

        auto offsetsIt = objectMappings.find(objectClass);
        if (offsetsIt == objectMappings.end()) {
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
            objectMappings[objectClass] = offsets;
            offset = offsets[method];
        } else {
            auto &offsets = offsetsIt->second;
            offset = (int)offsets.size() <= method ? -1 : offsets[method];
        }
    }

    if (offset == -1)
        constructAndThrow<&class_java_lang_NoSuchMethodError, init_java_lang_NoSuchMethodError>(ctx);

    return ((void **) object->vtable)[offset];
}

jobject gcAllocObject(jcontext ctx, jclass clazz, int mark) {
    if (!objects) {
        objects = new ankerl::unordered_dense::set<jobject>;
        rootObjects = new ankerl::unordered_dense::set<jobject>;
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
        .gcMark = mark,
        .vtable = (intptr_t) clazz->classVtable,
        .monitor = (intptr_t) new ObjectMonitor,
    };

    objectsLock.lock();
    if (mark == GC_MARK_START)
        objects->emplace(object);
    else
        rootObjects->emplace(object);
    objectsLock.unlock();

    return object;
}

/// Allocates an instance of a class. Caller must be at a safepoint, this calls the GC. Throws exceptions.
jobject gcAlloc(jcontext ctx, jclass clazz) {
    return gcAllocObject(ctx, clazz, GC_MARK_START);
}

/// Allocates an instance of a class and sets the GC mark to GC_MARK_NATIVE. Prefer storing objects on a stack frame. Throws exceptions.
jobject gcAllocProtected(jcontext ctx, jclass clazz) {
    return gcAllocObject(ctx, clazz, GC_MARK_PROTECTED);
}

jobject gcAllocEternal(jcontext ctx, jclass clazz) {
    return gcAllocObject(ctx, clazz, GC_MARK_ETERNAL);
}

static jobject makeRoot(jobject object, int mark) {
    objectsLock.lock();
    if (object->gcMark == mark) {
        objectsLock.unlock();
        return object;
    }
    object->gcMark = mark;
    objects->erase(object);
    rootObjects->emplace(object);
    objectsLock.unlock();
    return object;
}

static jobject makeRegular(jobject object, int mark) {
    objectsLock.lock();
    if (object->gcMark != mark) {
        objectsLock.unlock();
        return object;
    }
    object->gcMark = GC_MARK_START;
    rootObjects->erase(object);
    objects->emplace(object);
    objectsLock.unlock();
    return object;
}

jobject makeEternal(jobject object) {
    return makeRoot(object, GC_MARK_ETERNAL);
}

jobject makeEphemeral(jobject object) {
    return makeRegular(object, GC_MARK_ETERNAL);
}

jobject protectObject(jobject object) {
    return makeRoot(object, GC_MARK_PROTECTED);
}

jobject unprotectObject(jobject object) {
    return makeRegular(object, GC_MARK_PROTECTED);
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

static void collectionThreadFunc(jcontext ctx) {
    static std::vector<jobject> collected;

    try {
        FrameInfo frameInfo{ "GC:collect", 0 };
        auto frameRef = pushStackFrame(ctx, &frameInfo, nullptr, nullptr);

        while (true) { // Todo
            objectsLock.lock();
            if (!collectedObjects.empty()) {
                collected = collectedObjects;
                collectedObjects.clear();
            }
            objectsLock.unlock();

            if (!collected.empty()) {
                for (jobject obj : collected) {
                    tryCatch(frameRef, [&]{
                        ((finalizer_ptr)((void **)obj->vtable)[VTABLE_java_lang_Object_finalize])(ctx, obj);
                    }, &class_java_lang_Throwable, [](jobject ignored){});
                }

                for (jobject obj : collected) {
                    objectsLock.lock();
                    objects->erase(obj);
                    objectsLock.unlock();

                    heapUsage -= ((jclass) obj->clazz)->size + (int64_t) sizeof(ObjectMonitor);

                    delete (ObjectMonitor *) obj->monitor;

                    memset(obj, 0, sizeof(java_lang_Object)); // Erase collected objects to make memory bugs easier to catch
                    obj->gcMark = GC_MARK_COLLECTED; // Set collected flag again

                    delete[] (char *) obj;
                }

                collected.clear();
            }

        std::this_thread::sleep_for(std::chrono::milliseconds(1));

            SAFEPOINT();
        }
    } catch (ExitException &) { }

    ctx->dead = true;
    ctx->suspended = true;
}

/// Runs the garbage collector
void runGC(jcontext ctx) {
    static std::atomic_bool running;
    static std::mutex suspendMutex;

    if (running.exchange(true))
        return;

    FrameInfo frameInfo { "runGC", 0 };
    auto frameRef = pushStackFrame(ctx, &frameInfo, nullptr);

    auto blockTime = std::chrono::system_clock::now();

    // Suspend all threads before collecting (Suspended threads must have all owned objects reachable)
    {
        std::lock_guard lock(suspendMutex);
        suspendVM = true;
    }
    while (true) {
        if (exiting) throw ExitException();
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

    auto copyTime = std::chrono::system_clock::now();

    acquireCriticalLock();

    static jint mark;
    if (++mark > GC_MARK_END)
        mark = GC_MARK_START + 1;

    deepMarkedObjects.clear();

    auto nonCollectableTime = std::chrono::system_clock::now();

    // Explicitly mark children of non-collectable objects
    for (auto object : *rootObjects)
        ((gc_mark_ptr) ((jclass) object->clazz)->markFunction)(object, mark, GC_DEPTH_ALWAYS);

    auto markClassesTime = std::chrono::system_clock::now();

    // Mark class objects (Not in `objects`) // Todo: Not needed once all eternal
    for (auto &pair : *classes)
        mark_java_lang_Class((jobject) pair.second, mark, GC_DEPTH_ALWAYS);

    auto markStaticFieldsTime = std::chrono::system_clock::now();

    // Mark static fields
    for (auto &pair : *classes)
        ((gc_mark_ptr) pair.second->markFunction)(nullptr, mark, GC_DEPTH_ALWAYS);

    auto markStackTime = std::chrono::system_clock::now();

    // Mark stack objects
    for (auto threadContext : threadContexts) {
        for (int i = 0; i < threadContext->stackDepth; i++) {
            const auto &frame = threadContext->frames[i];
            for (int j = 0; j < (int)frame.info->size; j++) {
                const auto obj = frame.frame[j].o;
                if (objects->contains(obj))
                    ((gc_mark_ptr) ((jclass) obj->clazz)->markFunction)(obj, mark, 0);
            }
        }
    }

    auto markDeepTime = std::chrono::system_clock::now();

    // Specially mark deep object chains to avoid stack overflows
    while (!deepMarkedObjects.empty()) {
        auto deep = deepMarkedObjects;
        deepMarkedObjects.clear();
        for (auto obj : deep)
            ((gc_mark_ptr) ((jclass) obj->clazz)->markFunction)(obj, mark, 0);
    }

    auto collectTime = std::chrono::system_clock::now();

    // Collect unreachable objects
    for (jobject obj : *objects) {
        if ((obj->gcMark > GC_MARK_COLLECTED && obj->gcMark < GC_MARK_START) || obj->gcMark == mark)
            continue;
        obj->gcMark = GC_MARK_COLLECTED;
        collectedObjects.emplace_back(obj);
    }

#if false // Todo: Use macro
    std::map<jclass, int> usage;
    std::map<jclass, int> counts;
    std::multimap<int, jclass> usageMap;
    for (jobject o : *objectsSet) {
        auto cls = (jclass)o->clazz;
        counts[cls]++;
        usage[cls] += cls->size;
        if (cls->arrayDimensions > 0) {
            auto array = (jarray)o;
            auto component = (jclass)cls->componentClass;
            usage[cls] += cls->size + component->primitive ? component->size * array->length : (int)sizeof(jobject) * array->length;
        } else if (isInstance(ctx, o, &class_java_nio_ByteBuffer)) {
            auto buffer = (java_nio_ByteBuffer *) o;
            if (buffer->F_isOwner && buffer->parent.F_address)
                usage[cls] += buffer->parent.F_capacity;
        }
    }
    printf("GC collected %i objects after %i allocations and %i bytes (%i bytes and %i objects total)\n", (int)collectedObjects.size(), (int)allocationsSinceCollection, (int)heapUsage - (int)lastCollectionHeapUsage, (int)heapUsage, (int)objectsSet->size());
    for (auto &pair : usage)
        usageMap.emplace(pair.second, pair.first);
    int loggedUsages = 0;
    for (auto &pair : usageMap | std::views::reverse) {
        printf("%i objects (%i bytes) of %s\n", counts[pair.second], pair.first, (char *)pair.second->nativeName);
        if (++loggedUsages >= 10)
            break;
    }
    printf("\n");

    usage.clear();
    counts.clear();
    usageMap.clear();
    for (jobject o : collectedObjects) {
        auto cls = (jclass)o->clazz;
        counts[cls]++;
        usage[cls] += cls->size;
        if (cls->arrayDimensions > 0) {
            auto array = (jarray)o;
            auto component = (jclass)cls->componentClass;
            usage[cls] += cls->size + component->primitive ? component->size * array->length : (int)sizeof(jobject) * array->length;
        } else if (isInstance(ctx, o, &class_java_nio_ByteBuffer)) {
            auto buffer = (java_nio_ByteBuffer *) o;
            if (buffer->F_isOwner && buffer->parent.F_address)
                usage[cls] += buffer->parent.F_capacity;
        }
    }
    printf("Collection Stats:\n");
    for (auto &pair : usage)
        usageMap.emplace(pair.second, pair.first);
    loggedUsages = 0;
    for (auto &pair : usageMap | std::views::reverse) {
        printf("%i objects (%i bytes) of %s\n", counts[pair.second], pair.first, (char *)pair.second->nativeName);
        if (++loggedUsages >= 10)
            break;
    }
    printf("\n");
#endif

    allocationsSinceCollection = 0;

    releaseCriticalLock();
    {
        std::lock_guard lock(suspendMutex);
        suspendVM = false;
    }

#if false // Todo: Use macro
    auto finishTime = std::chrono::system_clock::now();

    printf("\nGC Timings:\n");
    printf("Block Time: %i\n", (int)std::chrono::duration_cast<std::chrono::milliseconds>(copyTime - blockTime).count());
    printf("Copy Time: %i\n", (int)std::chrono::duration_cast<std::chrono::milliseconds>(nonCollectableTime - copyTime).count());
    printf("Non-Collectable Time: %i\n", (int)std::chrono::duration_cast<std::chrono::milliseconds>(markClassesTime - nonCollectableTime).count());
    printf("Classes Time: %i\n", (int)std::chrono::duration_cast<std::chrono::milliseconds>(markStaticFieldsTime - markClassesTime).count());
    printf("Fields Time: %i\n", (int)std::chrono::duration_cast<std::chrono::milliseconds>(markStackTime - markStaticFieldsTime).count());
    printf("Stack Time: %i\n", (int)std::chrono::duration_cast<std::chrono::milliseconds>(markDeepTime - markStackTime).count());
    printf("Deep Time: %i\n", (int)std::chrono::duration_cast<std::chrono::milliseconds>(collectTime - markDeepTime).count());
    printf("Collect Time: %i\n", (int)std::chrono::duration_cast<std::chrono::milliseconds>(finishTime - collectTime).count());
    printf("Total Time: %i\n\n", (int)std::chrono::duration_cast<std::chrono::milliseconds>(finishTime - blockTime).count());
#endif

    lastCollectionHeapUsage = heapUsage;

    running = false;

    popStackFrame(ctx, nullptr);
}

void markDeepObject(jobject obj) {
    deepMarkedObjects.emplace_back(obj);
}

int64_t getHeapUsage() {
    return heapUsage;
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
    while (suspendVM) {
        if (exiting)
            throw ExitException();
    }
    ctx->suspended = false;
}

/// Pushes an exception frame onto the given stack frame. `type` can be null. Does not throw exceptions.
jmp_buf *pushExceptionFrame(jframe frame, jclass type) {
    if ((int)frame->exceptionFrames.size() < frame->exceptionFrameDepth + 1)
        frame->exceptionFrames.resize(frame->exceptionFrameDepth + 1);
    auto &exceptionFrame = frame->exceptionFrames[frame->exceptionFrameDepth++];
    exceptionFrame.type = type;
    return &exceptionFrame.landingPad;
}

/// Pops an exception frame then returns and clears the current exception. Does not throw exceptions.
jobject popExceptionFrame(jframe frame) {
    if (frame->exceptionFrameDepth == 0)
        throw std::runtime_error("No exception frame to pop");
    frame->exceptionFrameDepth--;
    return clearCurrentException(frame);
}

void popExceptionFrames(jframe frame, int count) {
    if (frame->exceptionFrameDepth < count)
        throw std::runtime_error("No exception frame to pop");
    frame->exceptionFrameDepth -= count;
}

jobject clearCurrentException(jframe frame) {
    auto exception = frame->exception;
    frame->exception = nullptr;
    return exception;
}

/// Throws an exception. This function does not return.
void throwException(jcontext ctx, jobject exception) {
    while (ctx->stackDepth > 0) {
        auto &frame = ctx->frames[ctx->stackDepth - 1];
        while (frame.exceptionFrameDepth > 0) {
            auto &exceptionFrame = frame.exceptionFrames[frame.exceptionFrameDepth - 1];
            if (!exceptionFrame.type or isInstance(ctx, exception, exceptionFrame.type)) {
                frame.exception = exception;
                longjmp(exceptionFrame.landingPad, 1);
            }
            frame.exceptionFrameDepth--;
        }
        if (frame.monitor) {
            monitorExit(ctx, frame.monitor); // Todo: Stack overflows if this throws an exception
            frame.monitor = nullptr;
        }
        ctx->stackDepth -= 1;
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

void throwStackOverflow(jcontext ctx) {
    constructAndThrow<&class_java_lang_StackOverflowError, init_java_lang_StackOverflowError>(ctx);
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
    constructAndThrow<&class_java_io_IOException, init_java_io_IOException>(ctx);
}

NORETURN void throwRuntimeException(jcontext ctx, const char *message) {
    if (message)
        constructAndThrowMsg<&class_java_lang_RuntimeException, init_java_lang_RuntimeException_java_lang_String>(ctx, message);
    constructAndThrow<&class_java_lang_RuntimeException, init_java_lang_RuntimeException>(ctx);
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
