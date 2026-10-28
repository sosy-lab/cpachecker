// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2018 The Nidhugg project
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: The ESBMC project
//
// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later

// Original file:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/pthread/fib_safe-7.i?ref_type=tags

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

typedef unsigned long int pthread_t;
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create (pthread_t *__restrict __newthread,
      const pthread_attr_t *__restrict __attr,
      void *(*__start_routine) (void *),
      void *__restrict __arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));

extern void abort(void);
void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "fib_safe.h", 13, __extension__ __PRETTY_FUNCTION__); })); }
void __VERIFIER_assert(int expression) { if (!expression) { ERROR: {reach_error();abort();}}; return; }
int i, j;
extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);
int p, q;
void *t1(void *arg) {
  for (p = 0; p < 7; p++) {
    __VERIFIER_atomic_begin();
    i = i + j;
    __VERIFIER_atomic_end();
  }
  return ((void *)0);
}
void *t2(void *arg) {
  for (q = 0; q < 7; q++) {
    __VERIFIER_atomic_begin();
    j = j + i;
    __VERIFIER_atomic_end();
  }
  return ((void *)0);
}
int cur = 1, prev = 0, next = 0;
int x;
int fib() {
  for (x = 0; x < 16; x++) {
    next = prev + cur;
    prev = cur;
    cur = next;
  }
  return prev;
}
int main(int argc, char **argv) {
  pthread_t id1, id2;
  __VERIFIER_atomic_begin();
  i = 1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  j = 1;
  __VERIFIER_atomic_end();
  pthread_create(&id1, ((void *)0), t1, ((void *)0));
  pthread_create(&id2, ((void *)0), t2, ((void *)0));
  int correct = fib();
  __VERIFIER_atomic_begin();
  _Bool assert_cond = i <= correct && j <= correct;
  __VERIFIER_atomic_end();
  __VERIFIER_assert(assert_cond);
  return 0;
}
