// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// Hand-condensed from the ensure_c_str_is_allocated pattern that the aws-c-common
// harnesses build their inputs with: a bounded allocation is written through, the
// address is stored in a struct, and a later block reads it back through that
// struct. The allocated base has to mean the same thing in the block that writes
// it and in the block whose violation condition reads it.

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *)
    __attribute__((__nothrow__, __leaf__)) __attribute__((__noreturn__));
void reach_error() { __assert_fail("0", "pointer_aliasing_malloc_safe.c", 3, "reach_error"); }
extern int __VERIFIER_nondet_int(void);
extern unsigned long __VERIFIER_nondet_ulong(void);
extern void *malloc(unsigned long);

struct buffer {
  unsigned long len;
  unsigned char *data;
};

int main(void) {
  unsigned long n = __VERIFIER_nondet_ulong();
  if (n == 0 || n > 8) {
    return 0;
  }

  unsigned char *p = malloc(n);
  if (p == 0) {
    return 0;
  }
  p[0] = 42;

  struct buffer b;
  b.data = p;
  b.len = n;

  if (__VERIFIER_nondet_int()) {
    if (b.data[0] != 42) {
      ERROR: { reach_error(); abort(); }
    }
  }
  return 0;
}
