#include "Clearwing.h"

#include "java/lang/Runnable.h"
#include "java/lang/Thread.h"
#include "java/lang/String.h"
#include "java/lang/Object.h"
#include "java/lang/Throwable.h"

extern "C" {

void threadEntrypoint(jcontext ctx, jthread thread) {
    // Todo

    jtype frame[1];
    auto frameRef = pushStackFrame(ctx, 1, frame, "java/lang/Thread:threadEntrypoint", nullptr);

    tryCatch(frameRef, [&]{
        if (thread->F_entrypoint) {
            frame[0].o = (jobject) createArray(ctx, &class_java_lang_String, 0);
            ((main_ptr) thread->F_entrypoint)(ctx, frame[0].o);
        } else if (thread->F_target)
            invokeInterface<func_java_lang_Runnable_run, &class_java_lang_Runnable, INDEX_java_lang_Runnable_run>(ctx, (jobject) thread->F_target);
    }, &class_java_lang_Throwable, [&](jobject ex){
        // Todo: Default handlers
    });

    popStackFrame(ctx);

    // Todo: Cleanup active threads list, delete context, delete thread (Deleting/joining the thread itself must be done in the destructor)
}

jobject SM_java_lang_Thread_currentThread_R_java_lang_Thread(jcontext ctx) {
    return (jobject) ctx->thread;
}

void SM_java_lang_Thread_sleepImpl_long_int(jcontext ctx, jlong millis, jint nanos) {
    auto end = std::chrono::system_clock::now() + std::chrono::milliseconds(millis) + std::chrono::nanoseconds(nanos);
    interruptedCheck(ctx);
    auto thread = (jobject) ctx->thread;
    while (std::chrono::system_clock::now() < end) {
        monitorEnter(ctx, thread);
        M_java_lang_Object_wait_long_int(ctx, thread, millis, nanos);
        monitorExit(ctx, thread);
        interruptedCheck(ctx);
    }
}

void M_java_lang_Thread_start(jcontext ctx, jobject self) {
    auto newContext = new Context;
    auto thread = (jthread) gcAlloc(newContext, &class_java_lang_Thread);
    thread->parent.gcMark = GC_MARK_ETERNAL;
    thread->F_nativeContext = (intptr_t) newContext;
    newContext->thread = thread;
    newContext->nativeThread = new std::thread;
    *newContext->nativeThread = std::thread(threadEntrypoint, newContext, thread);
}

void M_java_lang_Thread_interrupt(jcontext ctx, jobject self) {
    auto threadCtx = (jcontext) ((jthread) self)->F_nativeContext;
    threadCtx->lock.lock();
    threadCtx->thread->F_interrupted = true;
    if (threadCtx->blockedBy) {
        monitorEnter(ctx, threadCtx->blockedBy);
        M_java_lang_Object_notifyAll(ctx, threadCtx->blockedBy);
        monitorExit(ctx, threadCtx->blockedBy);
    }
    threadCtx->lock.unlock();
}

void M_java_lang_Thread_finalize(jcontext ctx, jobject selfObj) {
    auto self = (jthread) selfObj;
    // Todo: Cleanup of thread object data when thread execution finished
}

}
