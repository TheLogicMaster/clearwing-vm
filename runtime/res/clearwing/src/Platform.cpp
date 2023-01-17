#include "Clearwing.hpp"

#if !USE_PLATFORM_OVERRIDE

std::string vm::getOSLanguage() {
    return "en-US";
}

std::string vm::getSystemProperty(const std::string &key) {
    if (key == "os.name" || key == "os.arch")
        return "unknown";
    if (key == "line.separator")
        return "\n";
    return "";
}

#endif
