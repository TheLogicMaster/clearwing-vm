#include "Clearwing.hpp"
#include "Utils.hpp"
#include <java/io/FileOutputStream.hpp>
#include "java/io/FileNotFoundException.hpp"

void java::io::FileOutputStream::M_open(const jstring &nameObj, jbool append) {
    auto name = vm::getNativeString(nameObj);
    auto f = fopen(name, append ? "ab" : "wb");
    if (f)
        F_file = (jlong) f;
    else
        vm::throwExceptionCause<FileNotFoundException>(name);
}

void java::io::FileOutputStream::M_write(jint b, jbool append) {
    auto f = (FILE *) F_file;
    if (!f)
        vm::throwIOException("File closed");
    const char byte = (char) b;
    if (append)
        fseek(f, 0, SEEK_END);
    auto written = fwrite(&byte, 1, 1, f);
    if (written != 1)
        vm::throwNew<java::io::IOException>();
}

void java::io::FileOutputStream::M_writeBytes_Array1_byte(const jarray &b, jint off, jint len, jbool append) {
    auto f = (FILE *) F_file;
    if (!f)
        vm::throwIOException("File closed");
    if (append)
        fseek(f, 0, SEEK_END);
    auto written = (int)fwrite((char *) vm::checkedCast<vm::Array>(b)->data + off, 1, len, f);
    if (written != len)
        vm::throwNew<java::io::IOException>();
}

void java::io::FileOutputStream::M_close0() {
    auto f = (FILE *) F_file;
    if (!f)
        return;
    if (fclose(f))
        vm::throwNew<java::io::IOException>();
    F_file = 0;
}
