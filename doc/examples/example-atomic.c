// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
#include <pthread.h>
#include <stdatomic.h>

extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

void reach_error() {
  __assert_fail("0", "example-atomic.c", 15, "reach_error");
}

int a = 0;
int b = 0;
int c = 0;
int d = 0;
int e = 0;
int f = 0;
int g = 0;
int h = 0;

int r_out = 0;
int d_out = 0;
int e_out = 0;

int xcmp = 0;
int cmp_res = 0;

int done = 0;

void *thr1(void *_) {
  __atomic_store_n(&a, 1, 5);
  __atomic_store_n(&b, 2, 5);

  int r = __atomic_exchange_n(&c, 3, 5);
  __atomic_store_n(&r_out, r, 5);

  int oldd = __atomic_fetch_add(&d, 5, 5);
  __atomic_store_n(&d_out, oldd, 5);

  int olde = __atomic_fetch_sub(&e, 2, 5);
  __atomic_store_n(&e_out, olde, 5);

  __atomic_fetch_and(&f, 1, 5);
  __atomic_fetch_or(&g, 4, 5);
  __atomic_fetch_xor(&h, 7, 5);

  int expect = 0;
  int succ = __atomic_compare_exchange_n(&xcmp, &expect, 42, 0, 5, 5);
  __atomic_store_n(&cmp_res, succ, 5);

  __atomic_store_n(&done, 1, 5);

  return 0;
}

void *thr2(void *_) {
  if (__atomic_load_n(&a, 5) != 1) reach_error();
  if (__atomic_load_n(&b, 5) != 2) reach_error();
  if (__atomic_load_n(&c, 5) != 3) reach_error();
  if (__atomic_load_n(&d, 5) != 5) reach_error();
  if (__atomic_load_n(&e, 5) != -2) reach_error();
  if (__atomic_load_n(&f, 5) != 0) reach_error();
  if (__atomic_load_n(&g, 5) != 4) reach_error();
  if (__atomic_load_n(&h, 5) != 7) reach_error();

  if (__atomic_load_n(&r_out, 5) != 0) reach_error();
  if (__atomic_load_n(&d_out, 5) != 0) reach_error();
  if (__atomic_load_n(&e_out, 5) != 0) reach_error();

  if (__atomic_load_n(&xcmp, 5) != 42) reach_error();
  if (__atomic_load_n(&cmp_res, 5) != 1) reach_error();

  return 0;
}

// currently not easily solved by CPAchecker, but the threads can be tested separately
// int main() { 
//   unsigned long int t1, t2;
//   pthread_create(&t1, 0, thr1, 0);
//   pthread_join(t1, 0);
//   pthread_create(&t2, 0, thr2, 0);
//   pthread_join(t2, 0);
//   return 0;
// }

int main() {
  thr1(0);
  thr2(0);
  return 0;
}