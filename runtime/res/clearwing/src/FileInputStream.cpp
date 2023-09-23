#include "java/io/FileInputStream.h"
#include "java/io/FileNotFoundException.h"

extern "C" {

void M_java_io_FileInputStream_open_java_lang_String(jcontext ctx, jobject self, jobject nameObj) {
    auto stream = (java_io_FileInputStream *) NULL_CHECK(self);
    auto name = stringToNative(ctx, (jstring) NULL_CHECK(nameObj));
    auto f = fopen(name, "rb");
    if (f)
        stream->F_file = (jlong) f;
    else
        constructAndThrow<&class_java_io_FileNotFoundException, init_java_io_FileNotFoundException>(ctx);
}

jint M_java_io_FileInputStream_read_R_int(jcontext ctx, jobject self) {
    auto stream = (java_io_FileInputStream *) NULL_CHECK(self);
    auto f = (FILE *) stream->F_file;
    if (f)
        return fgetc(f);
    else
        throwIOException(ctx, "File closed");
    return 0;
}

jint M_java_io_FileInputStream_readBytes_Array1_byte_int_int_R_int(jcontext ctx, jobject self, jobject bytes, jint off, jint len) {
    auto stream = (java_io_FileInputStream *) NULL_CHECK(self);
    auto f = (FILE *) stream->F_file;
    if (f) {
        auto array = (jarray) NULL_CHECK(bytes);
        auto read = fread((char *) array->data + off, 1, len, f);
        if (!read)
            return -1;
        return (jint) read;
    } else
        throwIOException(ctx, "File closed");
    return 0;
}

jlong M_java_io_FileInputStream_skip_long_R_long(jcontext ctx, jobject self, jlong n) {
    auto stream = (java_io_FileInputStream *) NULL_CHECK(self);
    auto f = (FILE *) stream->F_file;
    if (!f)
        throwIOException(ctx, "File closed");
    auto pos = ftell(f);
    auto result = fseek(f, n, SEEK_CUR);
    if (result < 0)
        throwIOException(ctx, nullptr);
    return result - pos;
}

jint M_java_io_FileInputStream_available_R_int(jcontext ctx, jobject self) {
    auto stream = (java_io_FileInputStream *) NULL_CHECK(self);
    auto f = (FILE *) stream->F_file;
    if (!f)
        throwIOException(ctx, "File closed");
    auto pos = ftell(f);
    fseek(f, 0, SEEK_END);
    auto len = ftell(f);
    fseek(f, pos, SEEK_SET);
    return (jint) (len - pos);
}

void M_java_io_FileInputStream_close0(jcontext ctx, jobject self) {
    auto stream = (java_io_FileInputStream *) NULL_CHECK(self);
    auto f = (FILE *) stream->F_file;
    if (!f)
        return;
    if (fclose(f))
        throwIOException(ctx, nullptr);
    stream->F_file = 0;
}

}
