#include <Clearwing.hpp>
#include <java/lang/Double.hpp>
#include "Utils.hpp"
#include <cstring>

jlong java::lang::Double::SM_doubleToLongBits_R_long(jdouble value) {
    return bit_cast<jlong>(value);
}

jlong java::lang::Double::SM_doubleToRawLongBits_R_long(jdouble value) {
    return bit_cast<jlong>(value);
}

jdouble java::lang::Double::SM_longBitsToDouble_R_double(jlong value) {
    return bit_cast<jdouble>(value);
}

jstring java::lang::Double::SM_toStringImpl_R_java_lang_String(jdouble d, jbool b) {
    char s[32]{};
    if (!b) {
        snprintf(s, 32, "%lf", d);
    } else {
        snprintf(s, 32, "%1.20E", d);
    }

    // We need to match the format of Java spec.  That includes:
    // No "+" for positive exponent.
    // No leading zeroes in positive exponents.
    // No trailing zeroes in decimal portion.
    int j = 0;
    int i = 32;
    char s2[32]{};
    char inside = false;
    while (i-- > 0 && j < 32) {
        if (inside) {
            if (s[i] == '.') {
                s2[j++] = '0';
            }
            if (s[i] != '0') {
                inside = false;
                s2[j++] = s[i];
            }

        } else {
            if (s[i] == 'E') {
                inside = true;
            }
            if (s[i] == '+') {
                // If a positive exponent, we don't need leading zeroes in
                // the exponent
                while (s2[--j] == '0') {

                }
                j++;
                continue;
            }
            s2[j++] = s[i];
        }
    }
    i = 0;
    while (j-- > 0) {
        s[i++] = s2[j];
        if (s[i] == '\0') {
            break;
        }
    }
    if (strcmp(s, "NAN") == 0) {
        s[1] = 'a';
    }
    return vm::createString(s);
}
