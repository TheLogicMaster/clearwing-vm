#include <Clearwing.hpp>
#include <java/text/DateFormat.hpp>
#include "Utils.hpp"

jstring java::text::DateFormat::M_format_R_java_lang_String(const shared_ptr<java::util::Date> &param0, const shared_ptr<java::lang::StringBuffer> &param1) {
    return vm::createString("Date?");
}
