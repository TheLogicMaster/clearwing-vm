#include "java/util/zip/CRC32.h"
#include "java/nio/Buffer.h"

#include <zlib.h>

extern "C" {

jint SM_java_util_zip_CRC32_update_int_int_R_int(jcontext ctx, jint crc, jint b) {
    unsigned char byte = b;
    return (jint) crc32(crc, &byte, 1);
}

jint SM_java_util_zip_CRC32_updateBytes_int_Array1_byte_int_int_R_int(jcontext ctx, jint crc, jobject b, jint off, jint len) {
    return (jint)crc32(crc, (unsigned char *)((jarray) NULL_CHECK(b))->data + off, len);
}

jint SM_java_util_zip_CRC32_updateByteBuffer_int_java_nio_ByteBuffer_int_int_R_int(jcontext ctx, jint crc, jobject buffer, jint off, jint len) {
    return (jint)crc32(crc, (unsigned char *)((java_nio_Buffer *) NULL_CHECK(buffer))->F_address + off, len);
}

}
