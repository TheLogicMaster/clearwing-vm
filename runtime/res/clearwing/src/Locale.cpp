#include "java/util/Locale.h"

extern "C" {

jobject SM_java_util_Locale_getOSLanguage_R_java_lang_String(jcontext ctx) {
    static const char *language;
    if (!language) // Race condition here doesn't matter
        language = getOSLanguage();
    return (jobject) stringFromNative(ctx, language);
}

}
