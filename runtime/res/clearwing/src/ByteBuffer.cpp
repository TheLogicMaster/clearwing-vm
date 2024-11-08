#include "java/nio/ByteBuffer.h"
#include "java/nio/DirectCharBuffer.h"
#include "java/nio/DirectShortBuffer.h"
#include "java/nio/DirectIntBuffer.h"
#include "java/nio/DirectLongBuffer.h"
#include "java/nio/DirectFloatBuffer.h"
#include "java/nio/DirectDoubleBuffer.h"
#include "java/nio/BufferOverflowException.h"
#include "java/nio/BufferUnderflowException.h"

extern "C" {

static void throwBufferOverflow(jcontext ctx) {
    constructAndThrow<&class_java_nio_BufferOverflowException, init_java_nio_BufferOverflowException>(ctx);
}

static void throwBufferUnderflow(jcontext ctx) {
    constructAndThrow<&class_java_nio_BufferUnderflowException, init_java_nio_BufferUnderflowException>(ctx);
}

jobject SM_java_nio_ByteBuffer_allocateDirect_int_R_java_nio_ByteBuffer(jcontext ctx, jint size) {
    auto buffer = gcAllocProtected(ctx, &class_java_nio_ByteBuffer);
    auto data = new char[size];
    init_java_nio_ByteBuffer_long_int(ctx, buffer, (jlong) data, size);
    unprotectObject(buffer);
    adjustHeapUsage(size);
    return buffer;
}

void M_java_nio_ByteBuffer_deallocate(jcontext ctx, jobject self) {
    auto buffer = (java_nio_Buffer *)self;
    if (!buffer->F_address)
        return;
    delete[] (char *) buffer->F_address;
    adjustHeapUsage(-buffer->F_capacity);
    buffer->F_address = 0;
}

jbool SM_java_nio_ByteOrder_isLittleEndian_R_boolean(jcontext ctx) {
    return std::endian::native == std::endian::little; // NOLINT
}

jobject M_java_nio_Buffer_position_int_R_java_nio_Buffer(jcontext ctx, jobject self, jint position) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    if (position > buffer->F_limit or position < 0)
        throwIllegalArgument(ctx);
    if (buffer->F_mark > position)
        buffer->F_mark = -1;
    buffer->F_position = position;
    return self;
}

jobject M_java_nio_Buffer_limit_int_R_java_nio_Buffer(jcontext ctx, jobject self, jint limit) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    if (limit > buffer->F_capacity or limit < 0)
        throwIllegalArgument(ctx);
    buffer->F_limit = limit;
    if (buffer->F_position > limit)
        buffer->F_position = limit;
    if (buffer->F_mark > limit)
        buffer->F_mark = -1;
    return self;
}

jobject M_java_nio_FloatBuffer_position_int_R_java_nio_FloatBuffer(jcontext ctx, jobject self, jint position) {
    return M_java_nio_Buffer_position_int_R_java_nio_Buffer(ctx, self, position);
}

jobject M_java_nio_FloatBuffer_limit_int_R_java_nio_FloatBuffer(jcontext ctx, jobject self, jint limit) {
    return M_java_nio_Buffer_limit_int_R_java_nio_Buffer(ctx, self, limit);
}

jobject M_java_nio_ByteBuffer_position_int_R_java_nio_ByteBuffer(jcontext ctx, jobject self, jint position) {
    return M_java_nio_Buffer_position_int_R_java_nio_Buffer(ctx, self, position);
}

jobject M_java_nio_ByteBuffer_limit_int_R_java_nio_ByteBuffer(jcontext ctx, jobject self, jint limit) {
    return M_java_nio_Buffer_limit_int_R_java_nio_Buffer(ctx, self, limit);
}

jobject M_java_nio_ShortBuffer_position_int_R_java_nio_ShortBuffer(jcontext ctx, jobject self, jint position) {
    return M_java_nio_Buffer_position_int_R_java_nio_Buffer(ctx, self, position);
}

jobject M_java_nio_ShortBuffer_limit_int_R_java_nio_ShortBuffer(jcontext ctx, jobject self, jint limit) {
    return M_java_nio_Buffer_limit_int_R_java_nio_Buffer(ctx, self, limit);
}

jobject M_java_nio_IntBuffer_position_int_R_java_nio_IntBuffer(jcontext ctx, jobject self, jint position) {
    return M_java_nio_Buffer_position_int_R_java_nio_Buffer(ctx, self, position);
}

jobject M_java_nio_IntBuffer_limit_int_R_java_nio_IntBuffer(jcontext ctx, jobject self, jint limit) {
    return M_java_nio_Buffer_limit_int_R_java_nio_Buffer(ctx, self, limit);
}

jint M_java_nio_Buffer_nextGetIndex_R_int(jcontext ctx, jobject self) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    auto p = buffer->F_position;
    if (p >= buffer->F_limit)
        throwBufferUnderflow(ctx);
    buffer->F_position = p + 1;
    return p;
}

jint M_java_nio_Buffer_nextGetIndex_int_R_int(jcontext ctx, jobject self, jint nb) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    auto p = buffer->F_position;
    if (buffer->F_limit - p < nb)
        throwBufferUnderflow(ctx);
    buffer->F_position = p + nb;
    return p;
}

jint M_java_nio_Buffer_nextPutIndex_R_int(jcontext ctx, jobject self) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    auto p = buffer->F_position;
    if (p >= buffer->F_limit)
        throwBufferOverflow(ctx);
    buffer->F_position = p + 1;
    return p;
}

jint M_java_nio_Buffer_nextPutIndex_int_R_int(jcontext ctx, jobject self, jint nb) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    auto p = buffer->F_position;
    if (buffer->F_limit - p < nb)
        throwBufferOverflow(ctx);
    buffer->F_position = p + nb;
    return p;
}

jint M_java_nio_Buffer_checkIndex_int_R_int(jcontext ctx, jobject self, jint i) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    if (i < 0 or i >= buffer->F_limit)
        throwIndexOutOfBounds(ctx);
    return i;
}

jint M_java_nio_Buffer_checkIndex_int_int_R_int(jcontext ctx, jobject self, jint i, jint nb) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    if (i < 0 or nb > buffer->F_limit - i)
        throwIndexOutOfBounds(ctx);
    return i;
}

jbyte M_java_nio_ByteBuffer_get_R_byte(jcontext ctx, jobject self) {
    NULL_CHECK(self);
    return *(jbyte *)(((java_nio_Buffer *) self)->F_address + M_java_nio_Buffer_nextGetIndex_R_int(ctx, self));
}

jbyte M_java_nio_ByteBuffer_get_int_R_byte(jcontext ctx, jobject self, jint i) {
    NULL_CHECK(self);
    return *(jbyte *)(((java_nio_Buffer *) self)->F_address + M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i));
}

jobject M_java_nio_ByteBuffer_put_byte_R_java_nio_ByteBuffer(jcontext ctx, jobject self, jbyte byte) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    *(jbyte *)(buffer->F_address + M_java_nio_Buffer_nextPutIndex_R_int(ctx, self)) = byte;
    return self;
}

jobject M_java_nio_ByteBuffer_put_int_byte_R_java_nio_ByteBuffer(jcontext ctx, jobject self, jint i, jbyte byte) {
    auto buffer = (java_nio_Buffer *) NULL_CHECK(self);
    *(jbyte *)(buffer->F_address + M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i)) = byte;
    return self;
}

jint M_java_nio_DirectCharBuffer_ix_int_R_int(jcontext ctx, jobject self, jint i) {
    return (i << 1) + ((java_nio_DirectCharBuffer *) NULL_CHECK(self))->F_offset;
}

jchar M_java_nio_DirectCharBuffer_get_R_char(jcontext ctx, jobject self) {
    auto buffer = (java_nio_DirectCharBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextGetIndex_R_int(ctx, self);
    return *(jchar *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectCharBuffer_ix_int_R_int(ctx, self, i));
}

jchar M_java_nio_DirectCharBuffer_get_int_R_char(jcontext ctx, jobject self, jint i) {
    auto buffer = (java_nio_DirectCharBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    return *(jchar *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectCharBuffer_ix_int_R_int(ctx, self, i));
}

jchar M_java_nio_DirectCharBuffer_getUnchecked_int_R_char(jcontext ctx, jobject self, jint i) {
    auto buffer = (java_nio_DirectCharBuffer *) NULL_CHECK(self);
    return *(jchar *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectCharBuffer_ix_int_R_int(ctx, self, i));
}

jobject M_java_nio_DirectCharBuffer_put_char_R_java_nio_CharBuffer(jcontext ctx, jobject self, jchar x) {
    auto buffer = (java_nio_DirectCharBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextPutIndex_R_int(ctx, self);
    *(jchar *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectCharBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jobject M_java_nio_DirectCharBuffer_put_int_char_R_java_nio_CharBuffer(jcontext ctx, jobject self, jint i, jchar x) {
    auto buffer = (java_nio_DirectCharBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    *(jchar *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectCharBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jint M_java_nio_DirectShortBuffer_ix_int_R_int(jcontext ctx, jobject self, jint i) {
    return (i << 1) + ((java_nio_DirectShortBuffer *) NULL_CHECK(self))->F_offset;
}

jshort M_java_nio_DirectShortBuffer_get_R_short(jcontext ctx, jobject self) {
    auto buffer = (java_nio_DirectShortBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextGetIndex_R_int(ctx, self);
    return *(jshort *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectShortBuffer_ix_int_R_int(ctx, self, i));
}

jshort M_java_nio_DirectShortBuffer_get_int_R_short(jcontext ctx, jobject self, jint i) {
    auto buffer = (java_nio_DirectShortBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    return *(jshort *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectShortBuffer_ix_int_R_int(ctx, self, i));
}

jobject M_java_nio_DirectShortBuffer_put_short_R_java_nio_ShortBuffer(jcontext ctx, jobject self, jshort x) {
    auto buffer = (java_nio_DirectShortBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextPutIndex_R_int(ctx, self);
    *(jshort *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectShortBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jobject M_java_nio_DirectShortBuffer_put_int_short_R_java_nio_ShortBuffer(jcontext ctx, jobject self, jint i, jshort x) {
    auto buffer = (java_nio_DirectShortBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    *(jshort *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectShortBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jint M_java_nio_DirectIntBuffer_ix_int_R_int(jcontext ctx, jobject self, jint i) {
    return (i << 2) + ((java_nio_DirectIntBuffer *) NULL_CHECK(self))->F_offset;
}

jint M_java_nio_DirectIntBuffer_get_R_int(jcontext ctx, jobject self) {
    auto buffer = (java_nio_DirectIntBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextGetIndex_R_int(ctx, self);
    return *(jint *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectIntBuffer_ix_int_R_int(ctx, self, i));
}

jint M_java_nio_DirectIntBuffer_get_int_R_int(jcontext ctx, jobject self, jint i) {
    auto buffer = (java_nio_DirectIntBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    return *(jint *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectIntBuffer_ix_int_R_int(ctx, self, i));
}

jobject M_java_nio_DirectIntBuffer_put_int_R_java_nio_IntBuffer(jcontext ctx, jobject self, jint x) {
    auto buffer = (java_nio_DirectIntBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextPutIndex_R_int(ctx, self);
    *(jint *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectIntBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jobject M_java_nio_DirectIntBuffer_put_int_int_R_java_nio_IntBuffer(jcontext ctx, jobject self, jint i, jint x) {
    auto buffer = (java_nio_DirectIntBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    *(jint *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectIntBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jint M_java_nio_DirectLongBuffer_ix_int_R_int(jcontext ctx, jobject self, jint i) {
    return (i << 3) + ((java_nio_DirectLongBuffer *) NULL_CHECK(self))->F_offset;
}

jlong M_java_nio_DirectLongBuffer_get_R_long(jcontext ctx, jobject self) {
    auto buffer = (java_nio_DirectLongBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextGetIndex_R_int(ctx, self);
    return *(jlong *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectLongBuffer_ix_int_R_int(ctx, self, i));
}

jlong M_java_nio_DirectLongBuffer_get_int_R_long(jcontext ctx, jobject self, jint i) {
    auto buffer = (java_nio_DirectLongBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    return *(jlong *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectLongBuffer_ix_int_R_int(ctx, self, i));
}

jobject M_java_nio_DirectLongBuffer_put_long_R_java_nio_LongBuffer(jcontext ctx, jobject self, jlong x) {
    auto buffer = (java_nio_DirectLongBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextPutIndex_R_int(ctx, self);
    *(jlong *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectLongBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jobject M_java_nio_DirectLongBuffer_put_int_long_R_java_nio_LongBuffer(jcontext ctx, jobject self, jint i, jlong x) {
    auto buffer = (java_nio_DirectLongBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    *(jlong *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectLongBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jint M_java_nio_DirectFloatBuffer_ix_int_R_int(jcontext ctx, jobject self, jint i) {
    return (i << 2) + ((java_nio_DirectFloatBuffer *) NULL_CHECK(self))->F_offset;
}

jfloat M_java_nio_DirectFloatBuffer_get_R_float(jcontext ctx, jobject self) {
    auto buffer = (java_nio_DirectFloatBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextGetIndex_R_int(ctx, self);
    return *(jfloat *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectFloatBuffer_ix_int_R_int(ctx, self, i));
}

jfloat M_java_nio_DirectFloatBuffer_get_int_R_float(jcontext ctx, jobject self, jint i) {
    auto buffer = (java_nio_DirectFloatBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    return *(jfloat *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectFloatBuffer_ix_int_R_int(ctx, self, i));
}

jobject M_java_nio_DirectFloatBuffer_put_float_R_java_nio_FloatBuffer(jcontext ctx, jobject self, jfloat x) {
    auto buffer = (java_nio_DirectFloatBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextPutIndex_R_int(ctx, self);
    *(jfloat *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectFloatBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jobject M_java_nio_DirectFloatBuffer_put_int_float_R_java_nio_FloatBuffer(jcontext ctx, jobject self, jint i, jfloat x) {
    auto buffer = (java_nio_DirectFloatBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    *(jfloat *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectFloatBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jint M_java_nio_DirectDoubleBuffer_ix_int_R_int(jcontext ctx, jobject self, jint i) {
    return (i << 3) + ((java_nio_DirectDoubleBuffer *) NULL_CHECK(self))->F_offset;
}

jdouble M_java_nio_DirectDoubleBuffer_get_R_double(jcontext ctx, jobject self) {
    auto buffer = (java_nio_DirectDoubleBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextGetIndex_R_int(ctx, self);
    return *(jdouble *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectDoubleBuffer_ix_int_R_int(ctx, self, i));
}

jdouble M_java_nio_DirectDoubleBuffer_get_int_R_double(jcontext ctx, jobject self, jint i) {
    auto buffer = (java_nio_DirectDoubleBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    return *(jdouble *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectDoubleBuffer_ix_int_R_int(ctx, self, i));
}

jobject M_java_nio_DirectDoubleBuffer_put_double_R_java_nio_DoubleBuffer(jcontext ctx, jobject self, jdouble x) {
    auto buffer = (java_nio_DirectDoubleBuffer *) NULL_CHECK(self);
    auto i = M_java_nio_Buffer_nextPutIndex_R_int(ctx, self);
    *(jdouble *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectDoubleBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

jobject M_java_nio_DirectDoubleBuffer_put_int_double_R_java_nio_DoubleBuffer(jcontext ctx, jobject self, jint i, jdouble x) {
    auto buffer = (java_nio_DirectDoubleBuffer *) NULL_CHECK(self);
    i = M_java_nio_Buffer_checkIndex_int_R_int(ctx, self, i);
    *(jdouble *)(((java_nio_Buffer *) buffer->F_bb)->F_address + M_java_nio_DirectDoubleBuffer_ix_int_R_int(ctx, self, i)) = x;
    return self;
}

}
