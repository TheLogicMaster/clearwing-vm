#include "java/util/zip/Deflater.h"

#include <zlib.h>

extern "C" {

jlong SM_java_util_zip_Deflater_init_int_int_boolean_R_long(jcontext ctx, jint level, jint strategy, jbool nowrap) {
    auto stream = new z_stream{};
    if (deflateInit2(stream, level, Z_DEFLATED, nowrap ? -MAX_WBITS : MAX_WBITS, 8, strategy) != Z_OK) {
        delete stream;
        throwIOException(ctx, "Failed to initialize zlib stream");
        return 0;
    }
    return (jlong)stream;
}

jint M_java_util_zip_Deflater_deflateBytes_long_Array1_byte_int_int_int_R_int(jcontext ctx, jobject self, jlong address, jobject bytesObj, jint off, jint len, jint flush) {
    auto deflater = (java_util_zip_Deflater *) NULL_CHECK(self);
    auto bytes = (jarray) NULL_CHECK(bytesObj);
    auto stream = (z_streamp) address;
    stream->next_out = (unsigned char *) bytes->data + off;
    stream->avail_out = len;
    stream->next_in = ((unsigned char *) ((jarray) deflater->F_buf)->data) + deflater->F_off;
    stream->avail_in = deflater->F_len;
    uLong preIn = stream->total_in;
    uLong preOut = stream->total_out;
    int result = deflate(stream, deflater->F_finish ? Z_FINISH : 0);
    deflater->F_finished = result == Z_STREAM_END;
    deflater->F_len = (jint) stream->avail_in;
    deflater->F_off += (jint) (stream->total_in - preIn);
    return (jint)(stream->total_out - preOut);
}

jint SM_java_util_zip_Deflater_getAdler_long_R_int(jcontext ctx, jlong address) {
    return (jint)((z_streamp) address)->adler;
}

void SM_java_util_zip_Deflater_reset_long(jcontext ctx, jlong address) {
    deflateReset((z_streamp) address);
}

void SM_java_util_zip_Deflater_end_long(jcontext ctx, jlong address) {
    auto stream = (z_streamp) address;
    deflateEnd(stream);
    delete stream;
}

}
