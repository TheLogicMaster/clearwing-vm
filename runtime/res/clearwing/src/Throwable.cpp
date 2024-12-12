#include "java/lang/Throwable.h"
#include "java/lang/Thread.h"

#include <string>

extern "C" {

void M_java_lang_Throwable_fillInStack(jcontext ctx, jobject self) {
    auto throwable = (java_lang_Throwable *) NULL_CHECK(self);
    throwable->F_stackTrace = (jref)M_java_lang_Thread_getStackTrace_R_Array1_java_lang_StackTraceElement(ctx, (jobject)ctx->thread);
    std::string buffer = std::string((char *) jclass(self->clazz)->nativeName) + "\n";
    for (int i = (int) ctx->stackDepth - 1; i >= 0; i--) {
        auto &frame = ctx->frames[i];
        buffer += frame.method ? frame.method : "NULL";
        buffer += ":";
        buffer += std::to_string(frame.lineNumber) + "\n";
    }
    throwable->F_stack = (jref) stringFromNative(ctx, buffer.c_str());
}

}
