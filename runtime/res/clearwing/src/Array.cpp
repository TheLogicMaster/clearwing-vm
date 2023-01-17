#include "Array.hpp"
#include "map"
#include "java/lang/Class.hpp"

using namespace java::lang;

vm::Array::Array(const jclass &clazz, const std::vector<int> &dimensions) : Object(((ClassData *)vm::getArrayClass(clazz, (int)dimensions.size())->F_nativeData)->name), dimensions(dimensions) {
    clazz->M_ensureInitialized();
    if (clazz->F_primitive and dimensions.size() == 1) {
        auto array = new char[clazz->F_size * dimensions[0]]{};
        data = (void *)array;
        primitive = true;
    } else {
        auto array = new jobject[dimensions[0]];
        data = (void *)array;
        if (dimensions.size() > 1) {
            auto itemDims = std::vector<int>(dimensions.begin() + 1, dimensions.end());
            for (int i = 0; i < dimensions[0]; i++)
                array[i] = make_shared<Array>(clazz, itemDims);
        }
        primitive = false;
    }
    length = +dimensions[0]; // Unary '+' to remove incorrect warning
}

vm::Array::Array(const char *name, int dataSize, bool primitive, int size) : Object("[" + std::string(name)), dimensions({size}) {
    if (primitive)
        data = new char[dataSize * size]{};
    else
        data = new jobject[size];
    this->primitive = primitive;
    length = size;
}

vm::Array::~Array() {
    if (primitive)
        delete[] (char *)data;
    else
        delete[] (jobject *)data;
}
