#include <Clearwing.hpp>
#include <java/lang/Thread.hpp>
#include <java/lang/Thread_UncaughtExceptionHandler.hpp>
#include <mutex>
#include "java/lang/Throwable.hpp"
#include "java/lang/String.hpp"
#include <java/util/List.hpp>
#include "Utils.hpp"

using java::lang::Thread;

typedef void (*entrypoint_ptr)(jobject);

jthread Thread::SM_currentThread_R_java_lang_Thread() {
    return vm::threadData.thread;
}

void Thread::M_interrupt() {
    std::scoped_lock<std::mutex> lock(currentMonitorLock);
    F_interrupted = true;
    if (!F_currentMonitor)
        return;
    {
        SynchronizationObserver o(F_currentMonitor);
        F_currentMonitor->M_notifyAll();
    }
}

void Thread::SM_sleepImpl(jlong millis, int nanos) {
    auto thread = Thread::SM_currentThread_R_java_lang_Thread();
    auto end = std::chrono::system_clock::now() + std::chrono::milliseconds(millis) + std::chrono::nanoseconds(nanos);

    thread->interruptedCheck();
    while (std::chrono::system_clock::now() < end) {
        SynchronizationObserver o(thread);
        thread->M_wait(millis, nanos);
        thread->interruptedCheck();
    }
}

static void entrypoint(const jthread thread) {
    vm::threadData.thread = thread;

    try {
        if (thread->F_target)
            thread->F_target->M_run();
        else if (thread->F_entryPoint)
            ((entrypoint_ptr)thread->F_entryPoint)(vm::newArray(java::lang::String::CLASS, 0));
        else
            thread->M_run();
    } catch (jobject &exObj) {
        auto ex = vm::checkedCast<java::lang::Throwable>(exObj);
        shared_ptr<java::lang::Throwable> uncaught = nullptr;
        try {
            if (thread->F_uncaughtExceptionHandler)
                thread->F_uncaughtExceptionHandler->M_uncaughtException(thread, ex);
            else if (Thread::SF_defaultUncaughtExceptionHandler)
                Thread::SF_defaultUncaughtExceptionHandler->M_uncaughtException(thread, ex);
            else
                uncaught = ex;
        } catch (jobject &ex2) {
            uncaught = vm::checkedCast<java::lang::Throwable>(ex2);
        }
        if (uncaught) {
            printf("Uncaught Exception: \n");
            uncaught->M_printStackTrace();
        }
    }

    {
        SynchronizationObserver observer(Thread::CLASS);
        Thread::SF_activeThreads = Thread::SF_activeThreads - 1;
        thread->F_alive = false;
        {
            SynchronizationObserver o(thread);
            thread->M_notifyAll();
        }
        vm::threadData.thread = nullptr;
    }
}

void Thread::SM_cleanup() {
    SynchronizationObserver observer(Thread::CLASS);
    for (int i = 0; i < SF_threads->M_size_R_int(); i++) {
        auto t = vm::checkedCast<Thread>(SF_threads->M_get_R_java_lang_Object(i));
        if (t->F_alive)
            continue;
        auto nativeThread = (std::thread *)t->F_nativeThread;
        if (nativeThread->joinable())
            nativeThread->join();
        SF_threads->M_remove_R_java_lang_Object(i--);
    }
}

void Thread::M_start() {
    auto thread = object_cast<Thread>(get_this());

    {
        SynchronizationObserver observer(Thread::CLASS);
        SM_cleanup();
        Thread::SF_activeThreads = Thread::SF_activeThreads + 1;
        F_alive = true;
        SF_threads->M_add_R_boolean(thread);
    }

    if (F_entryPoint)
        entrypoint(thread);
    else {
        auto nativeThread = new std::thread;
        F_nativeThread = (jlong)nativeThread;
        *nativeThread = std::thread(entrypoint, thread);
    }
}

void Thread::M_finalize() {
    if (!F_nativeThread)
        return;
    auto thread = (std::thread *)F_nativeThread;
    if (thread->joinable())
        thread->join();
    delete thread;
}
