// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *)
    __attribute__((__nothrow__, __leaf__)) __attribute__((__noreturn__));
void reach_error() { __assert_fail("0", "setjmp.c", 13, "reach_error"); }
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
  ERROR: {reach_error(); abort();}
  }
}

typedef long int __jmp_buf[8];
typedef struct { unsigned long int __val[16]; } __sigset_t;
struct __jmp_buf_tag {
  __jmp_buf __jmpbuf;
  int __mask_was_saved;
  __sigset_t __saved_mask;
};
typedef struct __jmp_buf_tag jmp_buf[1];
extern int _setjmp(struct __jmp_buf_tag __env[1]) __attribute__((__nothrow__));
extern void longjmp(struct __jmp_buf_tag __env[1], int __val)
    __attribute__((__nothrow__)) __attribute__((__noreturn__));

jmp_buf env;
int counter = 0;

void step(int i) {
  if (i < 3) {
    counter++;
    longjmp(env, i + 1);
  }
}

int run(void) {
  volatile int result = 0;
  int i = _setjmp(env);
  step(i);
  result = i;
  return result;
}

int main() {
  int r = run();
  __VERIFIER_assert(r == 3);
  __VERIFIER_assert(counter == 3);
  r = run();
  // no longjmp, but setjmp is not active here
  step(r);
  __VERIFIER_assert(r == 3 && counter == 6);
  return 0;
}
