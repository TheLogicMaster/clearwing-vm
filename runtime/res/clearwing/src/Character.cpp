#include "Clearwing.h"

extern "C" {

jchar SM_java_lang_Character_toLowerCase_char_R_char(jcontext ctx, jchar c) {
    if ('A' <= c && c <= 'Z')
        return (jchar) (c + ('a' - 'A'));
    return c;
}

jint SM_java_lang_Character_toLowerCase_int_R_int(jcontext ctx, jint c) {
    if ('A' <= c && c <= 'Z')
        return (jchar) (c + ('a' - 'A'));
    return c;
}

}
