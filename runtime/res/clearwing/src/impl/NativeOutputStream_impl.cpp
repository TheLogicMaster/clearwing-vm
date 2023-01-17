#include "Clearwing.hpp"
#include <java/io/NativeOutputStream.hpp>
#include "Utils.hpp"

void java::io::NativeOutputStream::M_write_Array1_byte(const jarray &bytes, jint offset, jint length) {
    auto data = (char *)bytes->data;
    printf("%.*s", length, data + offset);
}
