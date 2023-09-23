#include "java/util/zip/Inflater.h"

#include <zlib.h>

extern "C" {

jlong SM_java_util_zip_Inflater_init_boolean_R_long(jcontext ctx, jbool nowrap) {
    auto stream = new z_stream{};
    if (inflateInit2(stream, nowrap ? -MAX_WBITS : MAX_WBITS) != Z_OK) {
        delete stream;
        throwIOException(ctx, "Failed to initialize zlib stream");
        return 0;
    }
    return (jlong)stream;
}

jint M_java_util_zip_Inflater_inflateBytes_long_Array1_byte_int_int_R_int(jcontext ctx, jobject self, jlong address, jobject b, jint off, jint len) {
    auto inflater = (java_util_zip_Inflater *) NULL_CHECK(self);
    auto stream = (z_streamp) address;
    auto bytes = (jarray) NULL_CHECK(b);
    stream->next_out = (unsigned char *) bytes->data + off;
    stream->avail_out = len;
    stream->next_in = ((unsigned char *) ((jarray) inflater->F_buf)->data) + inflater->F_off;
    stream->avail_in = inflater->F_len;
    uLong preIn = stream->total_in;
    uLong preOut = stream->total_out;
    int result = inflate(stream, Z_SYNC_FLUSH);
    switch (result) {
        case Z_NEED_DICT:
            throwIOException(ctx, "Dictionary needed");
        case Z_DATA_ERROR:
            throwIOException(ctx, "Data error");
        case Z_MEM_ERROR:
            throwIOException(ctx, "Memory error");
        case Z_STREAM_ERROR:
            throwIOException(ctx, "zlib stream error");
        default:
            break;
    }
    inflater->F_finished = result == Z_STREAM_END;
    inflater->F_len = (jint)stream->avail_in;
    inflater->F_off += (jint)(stream->total_in - preIn);
    return (jint)(stream->total_out - preOut);
}

jint SM_java_util_zip_Inflater_getAdler_long_R_int(jcontext ctx, jlong address) {
    return (jint) ((z_streamp) address)->adler;
}

void SM_java_util_zip_Inflater_reset_long(jcontext ctx, jlong address) {
    inflateReset((z_streamp) address);
}

void SM_java_util_zip_Inflater_end_long(jcontext ctx, jlong address) {
    auto stream = (z_streamp) address;
    inflateEnd(stream);
    delete stream;
}

}
