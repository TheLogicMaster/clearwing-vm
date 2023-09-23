#include "java/lang/Long.h"

#include <charconv>

extern "C" {

jobject SM_java_lang_Long_toString_long_int_R_java_lang_String(jcontext ctx, jlong value, jint radix) {
    std::string str;
    str.resize(30);
    std::to_chars(str.data(), str.data() + str.length(), value, radix);
    return (jobject) stringFromNative(ctx, str.c_str());
}

}
