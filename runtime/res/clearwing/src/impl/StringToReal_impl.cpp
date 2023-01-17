#include "Clearwing.hpp"
#include "java/lang/String.hpp"
#include "Utils.hpp"
#include <java/lang/StringToReal.hpp>
#include <java/lang/NumberFormatException.hpp>

jdouble java::lang::StringToReal::SM_parseDblImpl_R_double(const jstring &string, jint e) {
    auto length = string->F_count;
    auto chars = vm::checkedCast<vm::Array>(string->F_value)->getData<jchar>();
    char *data = new char[length + 1]{};
    for (int i = 0; i < length; i++)
        data[i] = (char)chars[i];
    char *err;
    double db = strtod(data, &err);
    if (data == err) {
        delete[] data;
        vm::throwEx(SM_invalidReal_R_java_lang_NumberFormatException(string, true));
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
