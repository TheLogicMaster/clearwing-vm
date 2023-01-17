#include "Clearwing.hpp"
#include "Utils.hpp"
#include <java/nio/ByteBuffer.hpp>
#include <java/nio/ByteOrder.hpp>
#include <java/nio/BufferOverflowException.hpp>
#include <java/nio/BufferUnderflowException.hpp>
#include <java/nio/DirectCharBuffer.hpp>
#include <java/nio/DirectShortBuffer.hpp>
#include <java/nio/DirectIntBuffer.hpp>
#include <java/nio/DirectLongBuffer.hpp>
#include <java/nio/DirectFloatBuffer.hpp>
#include <java/nio/DirectDoubleBuffer.hpp>
#include <java/lang/IndexOutOfBoundsException.hpp>
#include <java/lang/IllegalArgumentException.hpp>
#include "RuntimeTypes.hpp"
#include <bit>

using java::nio::ByteBuffer;
using java::nio::ByteOrder;
using java::nio::DirectCharBuffer;
using java::nio::DirectShortBuffer;
using java::nio::DirectIntBuffer;
using java::nio::DirectLongBuffer;
using java::nio::DirectFloatBuffer;
using java::nio::DirectDoubleBuffer;

shared_ptr<ByteBuffer> ByteBuffer::SM_allocateDirect_R_java_nio_ByteBuffer(jint size) {
    auto buffer = make_shared<ByteBuffer>();
    auto data = new char[size];
    buffer->init((jlong)data, size);
    return buffer;
}

void ByteBuffer::SM_deallocate(jlong ptr) {
    delete[] (char *)ptr;
}

jbool ByteOrder::SM_isLittleEndian_R_boolean() {
    return std::endian::native == std::endian::little;
}

shared_ptr<Buffer> Buffer::M_position_R_java_nio_Buffer(jint position) {
    if (position > F_limit or position < 0)
        vm::throwNew<java::lang::IllegalArgumentException>();
    if (F_mark > position)
        F_mark = -1;
    F_position = position;
    return object_cast<Buffer>(get_this());
}

shared_ptr<Buffer> Buffer::M_limit_R_java_nio_Buffer(jint limit) {
    if (limit > F_capacity or limit < 0)
        vm::throwNew<java::lang::IllegalArgumentException>();
    F_limit = limit;
    if (F_position > limit)
        F_position = limit;
    if (F_mark > limit)
        F_mark = -1;
    return object_cast<Buffer>(get_this());
}

shared_ptr<FloatBuffer> FloatBuffer::M_position_R_java_nio_FloatBuffer(jint position) {
    Buffer::M_position_R_java_nio_Buffer(position);
    return object_cast<FloatBuffer>(get_this());
}

shared_ptr<FloatBuffer> FloatBuffer::M_limit_R_java_nio_FloatBuffer(jint limit) {
    Buffer::M_limit_R_java_nio_Buffer(limit);
    return object_cast<FloatBuffer>(get_this());
}

shared_ptr<ByteBuffer> ByteBuffer::M_position_R_java_nio_ByteBuffer(jint position) {
    Buffer::M_position_R_java_nio_Buffer(position);
    return object_cast<ByteBuffer>(get_this());
}

shared_ptr<ByteBuffer> ByteBuffer::M_limit_R_java_nio_ByteBuffer(jint limit) {
    Buffer::M_limit_R_java_nio_Buffer(limit);
    return object_cast<ByteBuffer>(get_this());
}

shared_ptr<ShortBuffer> ShortBuffer::M_position_R_java_nio_ShortBuffer(jint position) {
    Buffer::M_position_R_java_nio_Buffer(position);
    return object_cast<ShortBuffer>(get_this());
}

shared_ptr<ShortBuffer> ShortBuffer::M_limit_R_java_nio_ShortBuffer(jint limit) {
    Buffer::M_limit_R_java_nio_Buffer(limit);
    return object_cast<ShortBuffer>(get_this());
}

shared_ptr<IntBuffer> IntBuffer::M_position_R_java_nio_IntBuffer(jint position) {
    Buffer::M_position_R_java_nio_Buffer(position);
    return object_cast<IntBuffer>(get_this());
}

shared_ptr<IntBuffer> IntBuffer::M_limit_R_java_nio_IntBuffer(jint limit) {
    Buffer::M_limit_R_java_nio_Buffer(limit);
    return object_cast<IntBuffer>(get_this());
}

jint Buffer::M_nextGetIndex_R_int() {
    jint p = F_position;
    if (p >= F_limit)
        vm::throwNew<BufferUnderflowException>();
    F_position = p + 1;
    return p;
}

jint Buffer::M_nextGetIndex_R_int(jint nb) {
    jint p = F_position;
    if (F_limit - p < nb)
        vm::throwNew<BufferUnderflowException>();
    F_position = p + nb;
    return p;
}

jint Buffer::M_nextPutIndex_R_int() {
    jint p = F_position;
    if (p >= F_limit)
        vm::throwNew<BufferOverflowException>();
    F_position = p + 1;
    return p;
}

jint Buffer::M_nextPutIndex_R_int(jint nb) {
    jint p = F_position;
    if (F_limit - p < nb)
        vm::throwNew<BufferOverflowException>();
    F_position = p + nb;
    return p;
}

jint Buffer::M_checkIndex_R_int(jint i) {
    if (i < 0 or i >= F_limit)
        vm::throwNew<java::lang::IndexOutOfBoundsException>();
    return i;
}

jint Buffer::M_checkIndex_R_int(jint i, jint nb) {
    if (i < 0 or nb > F_limit - i)
        vm::throwNew<java::lang::IndexOutOfBoundsException>();
    return i;
}

jbyte ByteBuffer::M_get_R_byte() {
    return *(jbyte *)(F_address + M_nextGetIndex_R_int());
}

jbyte ByteBuffer::M_get_R_byte(jint i) {
    return *(jbyte *)(F_address + M_checkIndex_R_int(i));
}

shared_ptr<ByteBuffer> ByteBuffer::M_put_R_java_nio_ByteBuffer(jbyte x) {
    *(jbyte *)(F_address + M_nextPutIndex_R_int()) = x;
    return object_cast<ByteBuffer>(get_this());
}

shared_ptr<ByteBuffer> ByteBuffer::M_put_R_java_nio_ByteBuffer(jint i, jbyte x) {
    *(jbyte *)(F_address + M_checkIndex_R_int(i)) = x;
    return object_cast<ByteBuffer>(get_this());
}

jint DirectCharBuffer::M_ix_R_int(jint i) {
    return (i << 1) + F_offset;
}

jchar DirectCharBuffer::M_get_R_char() {
    return *(jchar *)(F_bb->F_address + M_ix_R_int(M_nextGetIndex_R_int()));
}

jchar DirectCharBuffer::M_get_R_char(jint i) {
    return *(jchar *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i)));
}

jchar DirectCharBuffer::M_getUnchecked_R_char(jint i) {
    return *(jchar *)(F_bb->F_address + M_ix_R_int(i));
}

shared_ptr<CharBuffer> DirectCharBuffer::M_put_R_java_nio_CharBuffer(jchar x) {
    *(jchar *)(F_bb->F_address + M_ix_R_int(M_nextPutIndex_R_int())) = x;
    return object_cast<CharBuffer>(get_this());
}

shared_ptr<CharBuffer> DirectCharBuffer::M_put_R_java_nio_CharBuffer(jint i, jchar x) {
    *(jchar *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i))) = x;
    return object_cast<CharBuffer>(get_this());
}

jint DirectShortBuffer::M_ix_R_int(jint i) {
    return (i << 1) + F_offset;
}

jshort DirectShortBuffer::M_get_R_short() {
    return *(jshort *)(F_bb->F_address + M_ix_R_int(M_nextGetIndex_R_int()));
}

jshort DirectShortBuffer::M_get_R_short(jint i) {
    return *(jshort *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i)));
}

shared_ptr<ShortBuffer> DirectShortBuffer::M_put_R_java_nio_ShortBuffer(jshort x) {
    *(jshort *)(F_bb->F_address + M_ix_R_int(M_nextPutIndex_R_int())) = x;
    return object_cast<ShortBuffer>(get_this());
}

shared_ptr<ShortBuffer> DirectShortBuffer::M_put_R_java_nio_ShortBuffer(jint i, jshort x) {
    *(jshort *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i))) = x;
    return object_cast<ShortBuffer>(get_this());
}

jint DirectIntBuffer::M_ix_R_int(jint i) {
    return (i << 2) + F_offset;
}

jint DirectIntBuffer::M_get_R_int() {
    return *(jint *)(F_bb->F_address + M_ix_R_int(M_nextGetIndex_R_int()));
}

jint DirectIntBuffer::M_get_R_int(jint i) {
    return *(jint *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i)));
}

shared_ptr<IntBuffer> DirectIntBuffer::M_put_R_java_nio_IntBuffer(jint x) {
    *(jint *)(F_bb->F_address + M_ix_R_int(M_nextPutIndex_R_int())) = x;
    return object_cast<IntBuffer>(get_this());
}

shared_ptr<IntBuffer> DirectIntBuffer::M_put_R_java_nio_IntBuffer(jint i, jint x) {
    *(jint *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i))) = x;
    return object_cast<IntBuffer>(get_this());
}

jint DirectLongBuffer::M_ix_R_int(jint i) {
    return (i << 3) + F_offset;
}

jlong DirectLongBuffer::M_get_R_long() {
    return *(jlong *)(F_bb->F_address + M_ix_R_int(M_nextGetIndex_R_int()));
}

jlong DirectLongBuffer::M_get_R_long(jint i) {
    return *(jlong *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i)));
}

shared_ptr<LongBuffer> DirectLongBuffer::M_put_R_java_nio_LongBuffer(jlong x) {
    *(jlong *)(F_bb->F_address + M_ix_R_int(M_nextPutIndex_R_int())) = x;
    return object_cast<LongBuffer>(get_this());
}

shared_ptr<LongBuffer> DirectLongBuffer::M_put_R_java_nio_LongBuffer(jint i, jlong x) {
    *(jlong *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i))) = x;
    return object_cast<LongBuffer>(get_this());
}

jint DirectFloatBuffer::M_ix_R_int(jint i) {
    return (i << 2) + F_offset;
}

jfloat DirectFloatBuffer::M_get_R_float() {
    return *(jfloat *)(F_bb->F_address + M_ix_R_int(M_nextGetIndex_R_int()));
}

jfloat DirectFloatBuffer::M_get_R_float(jint i) {
    return *(jfloat *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i)));
}

shared_ptr<FloatBuffer> DirectFloatBuffer::M_put_R_java_nio_FloatBuffer(jfloat x) {
    *(jfloat *)(F_bb->F_address + M_ix_R_int(M_nextPutIndex_R_int())) = x;
    return object_cast<FloatBuffer>(get_this());
}

shared_ptr<FloatBuffer> DirectFloatBuffer::M_put_R_java_nio_FloatBuffer(jint i, jfloat x) {
    *(jfloat *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i))) = x;
    return object_cast<FloatBuffer>(get_this());
}

jint DirectDoubleBuffer::M_ix_R_int(jint i) {
    return (i << 3) + F_offset;
}

jdouble DirectDoubleBuffer::M_get_R_double() {
    return *(jdouble *)(F_bb->F_address + M_ix_R_int(M_nextGetIndex_R_int()));
}

jdouble DirectDoubleBuffer::M_get_R_double(jint i) {
    return *(jdouble *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i)));
}

shared_ptr<DoubleBuffer> DirectDoubleBuffer::M_put_R_java_nio_DoubleBuffer(jdouble x) {
    *(jdouble *)(F_bb->F_address + M_ix_R_int(M_nextPutIndex_R_int())) = x;
    return object_cast<DoubleBuffer>(get_this());
}

shared_ptr<DoubleBuffer> DirectDoubleBuffer::M_put_R_java_nio_DoubleBuffer(jint i, jdouble x) {
    *(jdouble *)(F_bb->F_address + M_ix_R_int(M_checkIndex_R_int(i))) = x;
    return object_cast<DoubleBuffer>(get_this());
}
