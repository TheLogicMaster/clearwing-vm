#include <Clearwing.hpp>
#include "Utils.hpp"
#include <java/lang/Throwable.hpp>
#include <algorithm>

void java::lang::Throwable::M_fillInStack() {
    std::string stack = name + "\n";
    for (int i = (int) vm::threadData.callStack.size() - 1; i >= 0; i--) {
        const auto &entry = vm::threadData.callStack[i];
        auto className = entry.className;
        std::replace(className.begin(), className.end(), '/', '.');
        stack += className + "." + entry.methodName + ":" + std::to_string(entry.line) + "\n";
    }
    F_stack = vm::createString(stack.c_str());
}
