#include "java/lang/Integer.h"

#include <charconv>

extern "C" {

jobject SM_java_lang_Integer_toString_int_int_R_java_lang_String(jcontext ctx, jint value, jint radix) {
    std::string str;
    str.resize(20);
    std::to_chars(str.data(), str.data() + str.length(), value, radix);
    return (jobject) stringFromNative(ctx, str.c_str());
}

}
