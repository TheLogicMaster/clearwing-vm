#include <Clearwing.hpp>
#include <java/util/zip/Inflater.hpp>
#include "Utils.hpp"

#include <zlib.h>

using java::util::zip::Inflater;

jlong Inflater::SM_init_R_long(jbool nowrap) {
    auto stream = new z_stream{};
    if (inflateInit2(stream, nowrap ? -MAX_WBITS : MAX_WBITS) != Z_OK)
        vm::throwIOException("Failed to initialize zlib stream");
    return (jlong)stream;
}

jint Inflater::M_inflateBytes_Array1_byte_R_int(jlong address, const jarray &b, jint off, jint len) {
    auto stream = (z_streamp)address;
    stream->next_out = (unsigned char *)b->data + off;
    stream->avail_out = len;
    stream->next_in = ((unsigned char *)(unsigned char *)F_buf->data) + F_off;
    stream->avail_in = F_len;
    uLong preIn = stream->total_in;
    uLong preOut = stream->total_out;
    int result = inflate(stream, Z_SYNC_FLUSH);
    switch (result) {
        case Z_NEED_DICT:
            vm::throwIOException("Dictionary needed");
        case Z_DATA_ERROR:
            vm::throwIOException("Data error");
        case Z_MEM_ERROR:
            vm::throwIOException("Memory error");
        case Z_STREAM_ERROR:
            vm::throwIOException("zlib stream error");
        default:
            break;
    }
    F_finished = result == Z_STREAM_END;
    F_len = (jint)stream->avail_in;
    F_off += (jint)(stream->total_in - preIn);
    return (jint)(stream->total_out - preOut);
}

jint Inflater::SM_getAdler_R_int(jlong address) {
    return (jint)((z_streamp)address)->adler;
}

void Inflater::SM_reset(jlong address) {
    inflateReset((z_streamp)address);
}

void Inflater::SM_end(jlong address) {
    auto stream = (z_streamp)address;
    inflateEnd(stream);
    delete stream;
}
