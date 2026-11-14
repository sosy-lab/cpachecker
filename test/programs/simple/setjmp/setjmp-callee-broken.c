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
int global = 0;

int bar(int v) {
  longjmp(env, v);
  return 8;
}

int foo(int v) {
  global = bar(v);
  return 8;
}

int main() {
  int local = 1;
  volatile int calls = 0;
  int ret = _setjmp(env);
  if (ret == 0) {
    calls++;
    global = foo(5);
    reach_error();
  }
  // the assignments of the results of bar() and foo() do not happen
  __VERIFIER_assert(global == 8);
  __VERIFIER_assert(ret == 5);
  __VERIFIER_assert(calls == 1);
  __VERIFIER_assert(local == 1);
  return 0;
}
