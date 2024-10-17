#include "java/nio/NativeUtils.h"

#include <cstring>

extern "C" {

jlong SM_java_nio_NativeUtils_getLong_long_R_long(jcontext ctx, jlong ptr) {
    return *(jlong *)ptr;
}

void SM_java_nio_NativeUtils_putLong_long_long(jcontext ctx, jlong ptr, jlong value) {
    *(jlong *)ptr = value;
}

jint SM_java_nio_NativeUtils_getInt_long_R_int(jcontext ctx, jlong ptr) {
    return *(jint *)ptr;
}

void SM_java_nio_NativeUtils_putInt_long_int(jcontext ctx, jlong ptr, jint value) {
    *(jint *)ptr = value;
}

jshort SM_java_nio_NativeUtils_getShort_long_R_short(jcontext ctx, jlong ptr) {
    return *(jshort *)ptr;
}

void SM_java_nio_NativeUtils_putShort_long_short(jcontext ctx, jlong ptr, jshort value) {
    *(jshort *)ptr = value;
}

jchar SM_java_nio_NativeUtils_getChar_long_R_char(jcontext ctx, jlong ptr) {
    return *(jchar *)ptr;
}

void SM_java_nio_NativeUtils_putChar_long_char(jcontext ctx, jlong ptr, jchar value) {
    *(jchar *)ptr = value;
}

jbyte SM_java_nio_NativeUtils_getByte_long_R_byte(jcontext ctx, jlong ptr) {
    return *(jbyte *)ptr;
}

void SM_java_nio_NativeUtils_putByte_long_byte(jcontext ctx, jlong ptr, jbyte value) {
    *(jbyte *)ptr = value;
}

void SM_java_nio_NativeUtils_copyMemory_long_long_long(jcontext ctx, jlong src, jlong dst, jlong bytes) {
    memcpy((void *)dst, (void *)src, bytes);
}

jlong SM_java_nio_NativeUtils_getArrayAddress_java_lang_Object_R_long(jcontext ctx, jobject array) {
    return (jlong) ((jarray) NULL_CHECK(array))->data;
}

jobject SM_java_nio_NativeUtils_getArrayClass_java_lang_Class_int_R_java_lang_Class(jcontext ctx, jobject type, jint dimensions) {
    return (jobject) getArrayClass((jclass) NULL_CHECK(type), dimensions);
}

}
