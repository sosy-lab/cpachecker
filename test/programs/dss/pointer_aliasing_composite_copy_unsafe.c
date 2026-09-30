// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// Copying a composite through a pointer writes every element of its array member,
// so one assignment writes the same memory region several times. Backward formula
// construction cannot encode that yet and rejects the edge, which makes this an
// UNKNOWN rather than the expected FALSE. The aws-c-common harnesses reach the
// same shape through memcpy, which is why they are out of reach as well.
//
// Once the retention constraints of an assignment are emitted once, excluding
// every cell any of its writes touches, this program has to come out FALSE.

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *)
    __attribute__((__nothrow__, __leaf__)) __attribute__((__noreturn__));
void reach_error() {
  __assert_fail("0", "pointer_aliasing_composite_copy_unsafe.c", 3, "reach_error");
}
extern int __VERIFIER_nondet_int(void);

struct holder {
  unsigned char buf[4];
};

int main(void) {
  struct holder a;
  struct holder b;
  struct holder *p = &a;

  a.buf[0] = 1;
  b.buf[0] = 9;
  b.buf[1] = 9;
  b.buf[2] = 9;
  b.buf[3] = 9;

  if (__VERIFIER_nondet_int()) {
    *p = b;
    if (a.buf[0] == 9) {
      ERROR: { reach_error(); abort(); }
    }
  }
  return 0;
}
