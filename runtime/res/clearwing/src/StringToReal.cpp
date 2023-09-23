#include "java/lang/StringToReal.h"
#include "java/lang/String.h"

extern "C" {

jdouble SM_java_lang_StringToReal_parseDblImpl_java_lang_String_int_R_double(jcontext ctx, jobject stringObj, jint e) {
    auto string = (jstring) stringObj;
    auto length = string->F_count;
    auto chars = (jchar *) jarray(string->F_value)->data;
    char *data = new char[length + 1]{};
    for (int i = 0; i < length; i++)
        data[i] = (char)chars[i];
    char *err;
    double db = strtod(data, &err);
    if (data == err) {
        delete[] data;
        throwException(ctx, SM_java_lang_StringToReal_invalidReal_java_lang_String_boolean_R_java_lang_NumberFormatException(ctx, stringObj, true));
        data = nullptr;
    }
    delete[] data;
    jlong exp = 1;
    if(e != 0) {
        if(e < 0) {
            while (e < -18) {
                // Long accumulator will overflow past 18 digits so we do
                // floating point math until we get there.
                // fixes https://github.com/codenameone/CodenameOne/issues/3250
                e++;
                db /= 10;
            }
            while(e < 0) {
                e++;
                exp *= 10;
            }
            db /= (double)exp;
        } else {
            while (e > 18) {
                // Long accumulator will overflow past 18 digits so we do
                // floating point math until we get there.
                // fixes https://github.com/codenameone/CodenameOne/issues/3250
                e--;
                db /= 10;
            }
            while(e > 0) {
                e--;
                exp *= 10;
            }
            db *= (double)exp;
        }
    }
    return db;
}

}
