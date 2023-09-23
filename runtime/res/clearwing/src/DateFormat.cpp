#include "Clearwing.h"
#include "java/text/DateFormat.h"

extern "C" {

jobject M_java_text_DateFormat_format_java_util_Date_java_lang_StringBuffer_R_java_lang_String(jcontext ctx, jobject self, jobject param0, jobject param1) {
    return (jobject) stringFromNative(ctx, "Date?");
}

}

