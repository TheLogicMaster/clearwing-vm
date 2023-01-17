#include <Clearwing.hpp>
#include <java/lang/Float.hpp>
#include <cstring>
#include "Utils.hpp"

jint java::lang::Float::SM_floatToIntBits_R_int(jfloat value) {
    return bit_cast<jint>(value);
}


jfloat java::lang::Float::SM_intBitsToFloat_R_float(jint value) {
    return bit_cast<jfloat>(value);
}

jstring java::lang::Float::SM_toStringImpl_R_java_lang_String(jfloat d, jbool b) {
    char s[32];
    if ( !b ){
        sprintf(s, "%f", d);
    } else {
        sprintf(s, "%1.20E", d);
    }
    // We need to match the format of Java spec.  That includes:
    // No "+" for positive exponent.
    // No leading zeroes in positive exponents.
    // No trailing zeroes in decimal portion.
    int j=0;
    int i=32;
    char s2[32];
    char inside=false;
    while (i-->0){
        if (inside){
            if (s[i]=='.'){
                s2[j++]='0';
            }
            if (s[i]!='0'){
                inside=false;
                s2[j++]=s[i];
            }

        } else {
            if (s[i]=='E'){
                inside=true;
            }
            if (s[i]=='+'){
                // If a positive exponent, we don't need leading zeroes in
                // the exponent
                while (s2[--j]=='0'){

                }
                j++;
                continue;
            }
            s2[j++]=s[i];
        }
    }
    i=0;
    while (j-->0){
        s[i++]=s2[j];
        if (s[i]=='\0'){
            break;
        }
    }
    if (strcmp(s, "NAN") == 0) {
        s[1] = 'a';
    }
    return vm::createString(s);
}
