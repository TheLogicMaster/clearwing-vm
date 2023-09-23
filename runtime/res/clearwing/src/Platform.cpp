#include "Clearwing.h"

extern "C" {

// Strings returned here are never freed but are cached, so it's fine

#if not USE_PLATFORM_OVERRIDE

const char *getOSLanguage() {
    return "en-US";
}

const char *getSystemProperty(const char *key) {
    std::string name(key);
    if (name == "os.name" || name == "os.arch")
        return "unknown";
    if (name == "line.separator")
        return "\n";
    return nullptr;
}

#endif

}
