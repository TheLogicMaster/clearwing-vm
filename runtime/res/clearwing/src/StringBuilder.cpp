#include "java/lang/StringBuilder.h"
#include "java/lang/String.h"
#include "java/lang/System.h"

extern "C" {

jobject M_java_lang_StringBuilder_append_char_R_java_lang_StringBuilder(jcontext ctx, jobject self, jchar c) {
    auto builder = (java_lang_StringBuilder *) NULL_CHECK(self);
    if (builder->F_count == jarray(builder->F_value)->length)
        M_java_lang_StringBuilder_enlargeBuffer_int(ctx, self, builder->F_count + 1);
    auto value = (jarray) builder->F_value;
    ((jchar *) value->data)[builder->F_count++] = c;
    return self;
}

jobject M_java_lang_StringBuilder_append_java_lang_Object_R_java_lang_StringBuilder(jcontext ctx, jobject self, jobject object) {
    if (!object)
        M_java_lang_StringBuilder_appendNull(ctx, self);
    else {
        jtype frame[1];
        FrameInfo frameInfo { "java/lang/StringBuilder:append", 1 };
        pushStackFrame(ctx, &frameInfo, frame);
        frame[0].o = invokeVirtual<func_java_lang_Object_toString_R_java_lang_String, VTABLE_java_lang_Object_toString_R_java_lang_String>(ctx, object);
        M_java_lang_StringBuilder_append_java_lang_String_R_java_lang_StringBuilder(ctx, self, frame[0].o);
        popStackFrame(ctx);
    }
    return self;
}

jobject M_java_lang_StringBuilder_append_java_lang_String_R_java_lang_StringBuilder(jcontext ctx, jobject self, jobject stringObj) {
    auto builder = (java_lang_StringBuilder *) NULL_CHECK(self);
    if (!stringObj) {
        M_java_lang_StringBuilder_appendNull(ctx, self);
        return self;
    }
    auto string = (jstring) stringObj;
    int newLength = builder->F_count + string->F_count;
    if (newLength > jarray(builder->F_value)->length)
        M_java_lang_StringBuilder_enlargeBuffer_int(ctx, self, newLength);
    M_java_lang_String_getChars_int_int_Array1_char_int(ctx, stringObj, 0, string->F_count, (jobject) builder->F_value, builder->F_count);
    builder->F_count = newLength;
    return self;
}

jchar M_java_lang_StringBuilder_charAt_int_R_char(jcontext ctx, jobject self, jint index) {
    auto builder = (java_lang_StringBuilder *) NULL_CHECK(self);
    auto array = jarray(builder->F_value);
    if (index < 0 or index >= array->length)
        throwIndexOutOfBounds(ctx);
    return ((jchar *) array->data)[index];
}

void M_java_lang_StringBuilder_getChars_int_int_Array1_char_int(jcontext ctx, jobject self, jint start, jint end, jobject dst, jint dstStart) {
    auto builder = (java_lang_StringBuilder *) NULL_CHECK(self);
    SM_java_lang_System_arraycopy_java_lang_Object_int_java_lang_Object_int_int(ctx, (jobject) builder->F_value, start, dst, dstStart, end - start);
}

}
