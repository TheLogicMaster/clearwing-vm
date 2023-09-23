#include "java/io/FileOutputStream.h"
#include "java/io/FileNotFoundException.h"

extern "C" {

void M_java_io_FileOutputStream_open_java_lang_String_boolean(jcontext ctx, jobject self, jobject nameObj, jbool append) {
    auto stream = (java_io_FileOutputStream *) NULL_CHECK(self);
    auto name = stringToNative(ctx, (jstring) NULL_CHECK(nameObj));
    auto f = fopen(name, append ? "ab" : "wb");
    if (f)
        stream->F_file = (jlong) f;
    else
        constructAndThrow<&class_java_io_FileNotFoundException, init_java_io_FileNotFoundException>(ctx);
}

void M_java_io_FileOutputStream_write_int_boolean(jcontext ctx, jobject self, jint b, jbool append) {
    auto stream = (java_io_FileOutputStream *) NULL_CHECK(self);
    auto f = (FILE *) stream->F_file;
    if (!f)
        throwIOException(ctx, "File closed");
    auto byte = (char) b;
    if (append)
        fseek(f, 0, SEEK_END);
    auto written = fwrite(&byte, 1, 1, f);
    if (written != 1)
        throwIOException(ctx, nullptr);
}

void M_java_io_FileOutputStream_writeBytes_Array1_byte_int_int_boolean(jcontext ctx, jobject self, jobject b, jint off, jint len, jbool append) {
    auto stream = (java_io_FileOutputStream *) NULL_CHECK(self);
    auto f = (FILE *) stream->F_file;
    if (!f)
        throwIOException(ctx, "File closed");
    if (append)
        fseek(f, 0, SEEK_END);
    auto bytes = (char *) ((jarray) b)->data;
    auto written = (jint) fwrite(bytes + off, 1, len, f);
    if (written != len)
        throwIOException(ctx, nullptr);
}

void M_java_io_FileOutputStream_close0(jcontext ctx, jobject self) {
    auto stream = (java_io_FileOutputStream *) NULL_CHECK(self);
    auto f = (FILE *) stream->F_file;
    if (!f)
        return;
    if (fclose(f))
        throwIOException(ctx, nullptr);
    stream->F_file = 0;
}

}
