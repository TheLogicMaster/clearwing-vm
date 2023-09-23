#include "java/io/NativeOutputStream.h"

extern "C" {

void M_java_io_NativeOutputStream_write_Array1_byte_int_int(jcontext ctx, jobject self, jobject bytes, jint offset, jint length) {
    auto data = (const char *) ((jarray) NULL_CHECK(bytes))->data;
    printf("%.*s", length, data + offset);
}

}
