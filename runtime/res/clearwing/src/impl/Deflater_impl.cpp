#include <Clearwing.hpp>
#include <java/util/zip/Deflater.hpp>
#include "Utils.hpp"

#include <zlib.h>

using java::util::zip::Deflater;

jlong Deflater::SM_init_R_long(jint level, jint strategy, jbool nowrap) {
    auto stream = new z_stream{};
    if (deflateInit2(stream, level, Z_DEFLATED, nowrap ? -MAX_WBITS : MAX_WBITS, 8, strategy) != Z_OK)
        vm::throwIOException("Failed to initialize zlib stream");
    return (jlong)stream;
}

jint Deflater::M_deflateBytes_Array1_byte_R_int(jlong address, const jarray &b, jint off, jint len, jint flush) {
    auto stream = (z_streamp)address;
    stream->next_out = (unsigned char *)b->data + off;
    stream->avail_out = len;
    stream->next_in = ((unsigned char *)F_buf->data) + F_off;
    stream->avail_in = F_len;
    uLong preIn = stream->total_in;
    uLong preOut = stream->total_out;
    int result = deflate(stream, F_finish ? Z_FINISH : 0);
    F_finished = result == Z_STREAM_END;
    F_len = (jint)stream->avail_in;
    F_off += (jint)(stream->total_in - preIn);
    return (jint)(stream->total_out - preOut);
}

jint Deflater::SM_getAdler_R_int(jlong address) {
    return (jint)((z_streamp)address)->adler;
}

void Deflater::SM_reset(jlong address) {
    deflateReset((z_streamp)address);
}

void Deflater::SM_end(jlong address) {
    auto stream = (z_streamp)address;
    deflateEnd(stream);
    delete stream;
}
