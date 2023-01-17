#include <Clearwing.hpp>
#include <java/lang/Character.hpp>

jchar java::lang::Character::SM_toLowerCase_R_char(jchar c) {
    if ('A' <= c && c <= 'Z')
        return (jchar) (c + ('a' - 'A'));
    return c;
}

jint java::lang::Character::SM_toLowerCase_R_int(jint c) {
    if ('A' <= c && c <= 'Z')
        return (jchar) (c + ('a' - 'A'));
    return c;
}
