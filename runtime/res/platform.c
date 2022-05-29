// This file is meant to be overridden

#include "cn1_globals.h"
#include <string.h>

JAVA_OBJECT getOSLanguage(CODENAME_ONE_THREAD_STATE) {
    return fromNativeString(threadStateData, "en-US");
}

JAVA_OBJECT getSystemProperty(CODENAME_ONE_THREAD_STATE, const char *key) {
    if (!strcmp(key, "os.name") || !strcmp(key, "os.arch"))
        return fromNativeString(threadStateData, "unknown");
    if (!strcmp(key, "line.separator"))
        return fromNativeString(threadStateData, "\n");
    return JAVA_NULL;
}
