// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// Hand-condensed from aws-c-common/aws_byte_buf_from_c_str_harness: a callee fills
// the fields of a struct through a pointer to the caller's local, and the caller
// then checks the same fields through another pointer to it. Encoding this needs
// the fields of the composite to be written through the aliased location, in both
// directions.

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *)
    __attribute__((__nothrow__, __leaf__)) __attribute__((__noreturn__));
void reach_error() { __assert_fail("0", "pointer_aliasing_struct_buffer_safe.c", 3, "reach_error"); }
extern unsigned long __VERIFIER_nondet_ulong(void);

struct buffer {
  unsigned long len;
  unsigned long capacity;
  unsigned char *data;
};

int buffer_is_valid(const struct buffer *b) {
  if (b->capacity == 0) {
    return b->len == 0 && b->data == 0;
  }
  return b->len <= b->capacity;
}

void buffer_init(struct buffer *b, unsigned char *data, unsigned long n) {
  b->data = n == 0 ? 0 : data;
  b->len = n;
  b->capacity = n;
}

int main(void) {
  unsigned char storage[8];
  struct buffer b;
  unsigned long n = __VERIFIER_nondet_ulong();

  if (n > 8) {
    return 0;
  }

  buffer_init(&b, storage, n);

  if (!buffer_is_valid(&b)) {
    ERROR: { reach_error(); abort(); }
  }
  return 0;
}
