#pragma once

#include <vector>

#include <java/lang/Object.hpp>
#include <java/lang/Class.hpp>
#include <java/lang/System.hpp>
#include <java/lang/Cloneable.hpp>
#include <java/lang/IndexOutOfBoundsException.hpp>
#include <java/lang/InternalError.hpp>

namespace vm {

class Array : public java::lang::Object, public virtual java::lang::Cloneable {
    public:
        Array(const jclass &clazz, const std::vector<int> &dimensions);

        Array(const char *name, int dataSize, bool primitive, int size);

        Array &operator=(const Array &) = delete;

        Array(const Array &) = delete;

        ~Array();

        inline jobject get_this() override { return Object::get_this(); }

        inline jobject clone() override {
            auto clazz = object_cast<java::lang::Class>(M_getClass_R_java_lang_Class());
            auto copy = make_shared<Array>(object_cast<java::lang::Class>(clazz->M_getComponentType_R_java_lang_Class()), dimensions);
            java::lang::System::SM_arraycopy(get_this(), 0, copy, 0, length);
            return copy;
        }

        template<typename T>
        inline T &get(int i) {
#if USE_VALUE_CHECKS
            if (i < 0 or i >= length)
                throw object_cast<java::lang::Object>(vm::newObject<java::lang::IndexOutOfBoundsException>());
            if (std::is_same<T, jobject>::value == primitive)
                throw object_cast<java::lang::Object>(vm::newObject<java::lang::InternalError>());
#endif
            return ((T *) data)[i];
        }

        template<typename T>
        inline T *getData() {
            return (T *) data;
        }

        void *data;
        bool primitive;
        jint length;
        std::vector<int> dimensions;
    };
}
