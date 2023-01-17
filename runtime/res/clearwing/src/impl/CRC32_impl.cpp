#include <Clearwing.hpp>
#include <java/util/zip/CRC32.hpp>
#include <java/nio/ByteBuffer.hpp>
#include "Array.hpp"
#include <Utils.hpp>

#include <zlib.h>

using java::util::zip::CRC32;

jint CRC32::SM_update_R_int(jint crc, jint b) {
    unsigned char byte = b;
    return (jint)crc32(crc, &byte, 1);
}

jint CRC32::SM_updateBytes_Array1_byte_R_int(jint crc, const jarray &b, jint off, jint len) {
    return (jint)crc32(crc, (unsigned char *)vm::checkedCast<vm::Array>(b)->data + off, len);
}

jint CRC32::SM_updateByteBuffer_R_int(jint crc, const shared_ptr<java::nio::ByteBuffer> &buffer, jint off, jint len) {
    return (jint)crc32(crc, (unsigned char*)buffer->F_address + off, len);
}
