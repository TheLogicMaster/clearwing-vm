#include "java/lang/Double.h"

#include <cstring>

extern "C" {

jlong SM_java_lang_Double_doubleToLongBits_double_R_long(jcontext ctx, jdouble value) {
    return bit_cast<jlong>(value);
}

jlong SM_java_lang_Double_doubleToRawLongBits_double_R_long(jcontext ctx, jdouble value) {
    return bit_cast<jlong>(value);
}

jdouble SM_java_lang_Double_longBitsToDouble_long_R_double(jcontext ctx, jlong value) {
    return bit_cast<jdouble>(value);
}

jobject SM_java_lang_Double_toStringImpl_double_boolean_R_java_lang_String(jcontext ctx, jdouble d, jbool b) {
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
    return (jobject) stringFromNative(ctx, s);
}

}
