#include "Clearwing.h"

#include "java/lang/Runnable.h"
#include "java/lang/Thread.h"
#include "java/lang/StackTraceElement.h"
#include "java/lang/String.h"
#include "java/lang/Object.h"
#include "java/lang/Throwable.h"

extern "C" {

void threadEntrypoint(jcontext ctx, jthread thread) {
    jtype frame[1];
    auto frameRef = pushStackFrame(ctx, 1, frame, "java/lang/Thread:threadEntrypoint", nullptr);

    thread->F_started = true;
    thread->F_alive = true;

    tryCatch(frameRef, [&]{
        if (thread->F_entrypoint) {
            frame[0].o = (jobject) createArray(ctx, &class_java_lang_String, 0);
            ((main_ptr) thread->F_entrypoint)(ctx, frame[0].o);
        }
        else
            invokeVirtual<func_java_lang_Thread_run, VTABLE_java_lang_Thread_run>(ctx, (jobject)thread);
    }, &class_java_lang_Throwable, [&](jobject ex){
        // Todo: Default handlers
        tryCatch(frameRef, [&] {
            auto throwable = (java_lang_Throwable *)ex;
            if (throwable->F_message)
                printf("Uncaught Exception: %s\n", stringToNative(ctx, (jstring)throwable->F_message));
            INVOKE_VIRTUAL(java_lang_Throwable_printStackTrace, ex);
        }, &class_java_lang_Throwable, [&](jobject) {});
    });

    popStackFrame(ctx);

    thread->F_alive = false;

    unprotectObject((jobject)thread);
}

jobject SM_java_lang_Thread_currentThread_R_java_lang_Thread(jcontext ctx) {
    return (jobject) ctx->thread;
}

void SM_java_lang_Thread_sleepImpl_long_int(jcontext ctx, jlong millis, jint nanos) {
    auto end = std::chrono::system_clock::now() + std::chrono::milliseconds(millis) + std::chrono::nanoseconds(nanos);
    interruptedCheck(ctx);
    auto thread = (jobject) ctx->thread;
    while (std::chrono::system_clock::now() < end) {
        auto remaining = end - std::chrono::system_clock::now();
        auto remainingNanos = duration_cast<std::chrono::nanoseconds>(remaining);
        auto remainingMillis = std::chrono::round<std::chrono::milliseconds>(remaining);
        remainingNanos -= remainingMillis;
        monitorEnter(ctx, thread);
        M_java_lang_Object_wait_long_int(ctx, thread, remainingMillis.count(), (int) remainingNanos.count());
        monitorExit(ctx, thread);
        interruptedCheck(ctx);
    }
}

void M_java_lang_Thread_start(jcontext ctx, jobject self) {
    auto newContext = createContext();
    auto thread = (jthread) self;
    protectObject((jobject)thread);
    thread->F_nativeContext = (intptr_t) newContext;
    newContext->thread = thread;
    newContext->nativeThread = new std::thread;
    *newContext->nativeThread = std::thread(threadEntrypoint, newContext, thread);
}

void M_java_lang_Thread_interrupt(jcontext ctx, jobject self) {
    auto threadCtx = (jcontext) ((jthread) NULL_CHECK(self))->F_nativeContext;
    threadCtx->lock.lock();
    threadCtx->thread->F_interrupted = true;
    if (threadCtx->blockedBy)
        ((jmonitor) ((jobject) threadCtx->blockedBy)->monitor)->condition.notify_all();
    threadCtx->lock.unlock();
}

void M_java_lang_Thread_finalize(jcontext ctx, jobject selfObj) {
    auto self = (jthread) selfObj;
    auto context = (jcontext) self->F_nativeContext;
    context->nativeThread->join();
    delete context->nativeThread;
    destroyContext(context);
}

jobject M_java_lang_Thread_getStackTrace_R_Array1_java_lang_StackTraceElement(jcontext ctx, jobject self) {
    jtype frame[4];
    auto frameRef = pushStackFrame(ctx, 1, frame, "java/lang/Thread:getStackTrace", nullptr);

    jarray trace = createArray(ctx, &class_java_lang_StackTraceElement, ctx->stackDepth);
    frame[0].o = (jobject)trace;

    for (int i = 0; i < ctx->stackDepth; i++) {
        auto stackFrame = &ctx->frames[i];
        std::string_view method = stackFrame->method ? stackFrame->method : "";
        auto separator = method.find(':');
        frame[1].o = (jobject)stringFromNative(ctx, separator != std::string_view::npos ? method.substr(0, separator) : "Unknown");
        frame[2].o = (jobject)stringFromNative(ctx, separator != std::string_view::npos ? method.substr(separator + 1) : "unknown");
        frame[3].o = frame[1].o;
        ((jobject *)trace->data)[ctx->stackDepth - 1 - i] = constructObject<&class_java_lang_StackTraceElement, init_java_lang_StackTraceElement_java_lang_String_java_lang_String_java_lang_String_int>(ctx, frame[1].o, frame[2].o, frame[3].o, stackFrame->lineNumber);
    }

    popStackFrame(ctx);
    return frame[0].o;
}

}
