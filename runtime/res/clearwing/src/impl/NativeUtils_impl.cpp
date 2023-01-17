#include <Clearwing.hpp>
#include <java/nio/NativeUtils.hpp>
#include <cstring>
#include "Utils.hpp"

jlong java::nio::NativeUtils::SM_getLong_R_long(jlong ptr) {
    return *(jlong *)ptr;
}

void java::nio::NativeUtils::SM_putLong(jlong ptr, jlong value) {
    *(jlong *)ptr = value;
}

jint java::nio::NativeUtils::SM_getInt_R_int(jlong ptr) {
    return *(jint *)ptr;
}

void java::nio::NativeUtils::SM_putInt(jlong ptr, jint value) {
    *(jint *)ptr = value;
}

jshort java::nio::NativeUtils::SM_getShort_R_short(jlong ptr) {
    return *(jshort *)ptr;
}

void java::nio::NativeUtils::SM_putShort(jlong ptr, jshort value) {
    *(jshort *)ptr = value;
}

jchar java::nio::NativeUtils::SM_getChar_R_char(jlong ptr) {
    return *(jchar *)ptr;
}

void java::nio::NativeUtils::SM_putChar(jlong ptr, jchar value) {
    *(jchar *)ptr = value;
}

jbyte java::nio::NativeUtils::SM_getByte_R_byte(jlong ptr) {
    return *(jbyte *)ptr;
}

void java::nio::NativeUtils::SM_putByte(jlong ptr, jbyte value) {
    *(jbyte *)ptr = value;
}

void java::nio::NativeUtils::SM_copyMemory(jlong src, jlong dst, jlong bytes) {
    memcpy((void *)dst, (void *)src, bytes);
}

jlong java::nio::NativeUtils::SM_getArrayAddress_R_long(const jobject &array) {
    return (jlong)vm::checkedCast<vm::Array>(array)->data;
}
