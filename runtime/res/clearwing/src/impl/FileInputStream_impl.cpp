#include "Clearwing.hpp"
#include "Utils.hpp"
#include <java/io/FileInputStream.hpp>
#include <java/io/FileNotFoundException.hpp>

void java::io::FileInputStream::M_open(const jstring &nameObj) {
    auto name = vm::getNativeString(nameObj);
    auto f = fopen(name, "rb");
    if (f)
        F_file = (jlong) f;
    else
        vm::throwExceptionCause<FileNotFoundException>(name);
}

jint java::io::FileInputStream::M_read_R_int() {
    auto f = (FILE *) F_file;
    if (f)
        return fgetc(f);
    else
        vm::throwIOException("File closed");
    return 0;
}

jint java::io::FileInputStream::M_readBytes_Array1_byte_R_int(const jarray &b, jint off, jint len) {
    auto f = (FILE *) F_file;
    if (f) {
        auto array = vm::checkedCast<vm::Array>(b);
        auto read = fread((char *) array->data + off, 1, len, f);
        if (!read)
            return -1;
        return (int) read;
    } else
        vm::throwIOException("File closed");
    return 0;
}

jlong java::io::FileInputStream::M_skip_R_long(jlong n) {
    auto f = (FILE *) F_file;
    if (!f)
        vm::throwIOException("File closed");
    auto pos = ftell(f);
    auto result = fseek(f, n, SEEK_CUR);
    if (result < 0)
        vm::throwNew<java::io::IOException>();
    return result - pos;
}

jint java::io::FileInputStream::M_available_R_int() {
    auto f = (FILE *) F_file;
    if (!f)
        vm::throwIOException("File closed");
    auto pos = ftell(f);
    fseek(f, 0, SEEK_END);
    auto len = ftell(f);
    fseek(f, pos, SEEK_SET);
    return (int) (len - pos);
}

void java::io::FileInputStream::M_close0() {
    auto f = (FILE *) F_file;
    if (!f)
        return;
    if (fclose(f))
        vm::throwNew<java::io::IOException>();
    F_file = 0;
}
