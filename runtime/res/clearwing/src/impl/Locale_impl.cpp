#include <Clearwing.hpp>
#include "Utils.hpp"
#include <java/util/Locale.hpp>

jstring java::util::Locale::SM_getOSLanguage_R_java_lang_String() {
    auto language = vm::getOSLanguage();
    return vm::createString(language.c_str());
}
