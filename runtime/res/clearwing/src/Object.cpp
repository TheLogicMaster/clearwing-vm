#include "Clearwing.h"
#include "java/lang/Object.h"

extern "C" {

void clinit_java_lang_Object(jcontext ctx) {
}

void mark_java_lang_Object(jobject object, jint mark) {
    if (object and object->gcMark >= GC_MARK_START)
        object->gcMark = mark;
}

void init_java_lang_Object(jcontext ctx, jobject self) {
}

jint M_java_lang_Object_hashCode_R_int(jcontext ctx, jobject self) {
    return (jint) (intptr_t) self;
}

jbool M_java_lang_Object_equals_java_lang_Object_R_boolean(jcontext ctx, jobject self, jobject other) {
    return self == other;
}

jobject M_java_lang_Object_clone_R_java_lang_Object(jcontext ctx, jobject self) {
    return nullptr; // Todo
}

jobject M_java_lang_Object_getClass_R_java_lang_Class(jcontext ctx, jobject self) {
    return (jobject) self->clazz;
}

jobject M_java_lang_Object_toString_R_java_lang_String(jcontext ctx, jobject self) {
    auto string = std::string("Object$") + std::to_string((intptr_t) self);
    return (jobject) stringFromNative(ctx, string.c_str());
}

void M_java_lang_Object_finalize(jcontext ctx, jobject self) {
}

void M_java_lang_Object_notify(jcontext ctx, jobject self) {
    monitorOwnerCheck(ctx, self);
    ((jmonitor) self->monitor)->condition.notify_one();
}

void M_java_lang_Object_notifyAll(jcontext ctx, jobject self) {
    monitorOwnerCheck(ctx, self);
    ((jmonitor) self->monitor)->condition.notify_all();
}

void M_java_lang_Object_wait(jcontext ctx, jobject self) {
    monitorOwnerCheck(ctx, self);
    interruptedCheck(ctx);
    ctx->lock.lock();
    ctx->blockedBy = self;
    ctx->lock.unlock();
    monitorExit(ctx, self);

    ctx->suspended = true;
    auto monitor = (jmonitor) self->monitor;
    {
        std::unique_lock<std::mutex> lock(monitor->conditionMutex);
        monitor->condition.wait(lock);
    }
    ctx->suspended = false;
    SAFEPOINT();

    monitorEnter(ctx, self);
    ctx->lock.lock();
    ctx->blockedBy = nullptr;
    ctx->lock.unlock();
    interruptedCheck(ctx);
}

void M_java_lang_Object_wait_long(jcontext ctx, jobject self, jlong millis) {
    M_java_lang_Object_wait_long_int(ctx, self, millis, 0);
}

void M_java_lang_Object_wait_long_int(jcontext ctx, jobject self, jlong millis, jint nanos) {
    monitorOwnerCheck(ctx, self);
    interruptedCheck(ctx);
    ctx->lock.lock();
    ctx->blockedBy = self;
    ctx->lock.unlock();
    monitorExit(ctx, self);

    ctx->suspended = true;
    auto monitor = (jmonitor) self->monitor;
    {
        std::unique_lock<std::mutex> lock(monitor->conditionMutex);
        monitor->condition.wait_for(lock, std::chrono::milliseconds(millis) + std::chrono::nanoseconds(nanos));
    }
    ctx->suspended = false;
    SAFEPOINT();

    monitorEnter(ctx, self);
    ctx->lock.lock();
    ctx->blockedBy = nullptr;
    ctx->lock.unlock();
    interruptedCheck(ctx);
}

static void *vtable_java_lang_Object[] {
        (void *) M_java_lang_Object_hashCode_R_int,
        (void *) M_java_lang_Object_equals_java_lang_Object_R_boolean,
        (void *) M_java_lang_Object_clone_R_java_lang_Object,
        (void *) M_java_lang_Object_getClass_R_java_lang_Class,
        (void *) M_java_lang_Object_toString_R_java_lang_String,
        (void *) M_java_lang_Object_finalize,
        (void *) M_java_lang_Object_notify,
        (void *) M_java_lang_Object_notifyAll,
        (void *) M_java_lang_Object_wait,
        (void *) M_java_lang_Object_wait_long,
        (void *) M_java_lang_Object_wait_long_int,
};

static MethodMetadata methodMetadata[] {
        { "<init>", (intptr_t) init_java_lang_Object, "()V", 0x400 },
        { "hashCode", 0, "()I", 0x400 },
        { "equals", 1, "(Ljava/lang/Object;)Z", 0x400 },
        { "clone", 2, "()Ljava/lang/Object;", 0x400 },
        { "getClass", 3, "()Ljava/lang/Class;", 0x400 },
        { "toString", 4, "()Ljava/lang/String;", 0x400 },
        { "finalize", 5, "()V", 0x400 },
        { "notify", 6, "()V", 0x400 },
        { "notifyAll", 7, "()V", 0x400 },
        { "wait", 8, "()V", 0x400 },
        { "wait", 9, "(L)V", 0x400 },
        { "wait", 10, "(LI)V", 0x400 },
};

Class class_java_lang_Object {
        .nativeName = (intptr_t) "java/lang/Object",
        .parentClass = (intptr_t) nullptr,
        .size = sizeof(java_lang_Object),
        .classVtable = (intptr_t) vtable_java_lang_Object,
        .staticInitializer = (intptr_t) clinit_java_lang_Object,
        .markFunction = (intptr_t) mark_java_lang_Object,
        .primitive = false,
        .arrayDimensions = 0,
        .componentClass = (intptr_t) nullptr,
        .access = 0x400,
        .interfaceCount = 0,
        .nativeInterfaces = (intptr_t) nullptr,
        .fieldCount = 0,
        .nativeFields = (intptr_t) nullptr,
        .methodCount = 12,
        .nativeMethods = (intptr_t) methodMetadata,
        .anonymous = false,
        .synthetic = false,
};

static bool registered_java_lang_Object = registerClass(&class_java_lang_Object);

}
