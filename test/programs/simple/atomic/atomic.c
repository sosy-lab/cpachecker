// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
#include <pthread.h>
#include <stdatomic.h>

extern void reach_error(void);

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

// operands and results of the __atomic_op_fetch builtins, which yield the updated value
int add_v = 0;
int sub_v = 10;
int and_v = 12;
int or_v = 12;
int xor_v = 12;

int add_new = 0;
int sub_new = 0;
int and_new = 0;
int or_new = 0;
int xor_new = 0;

// the NAND builtins, which compute ~(*ptr & value)
int fn = 12;
int nf = 12;
int nand_old = 0;
int nand_new = 0;

// the generic builtins, which pass their operands by pointer
int gsrc = 7;
int gdst = 0;
int gload = 0;
int gx = 1;
int gxval = 9;
int gxold = 0;
int gcmp = 5;
int gcmp_res = 0;

// the flag builtins
char flag = 0;
int tas_first = 0;
int tas_second = 0;
int flag_after_clear = 1;

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

  add_new = __atomic_add_fetch(&add_v, 3, 5);
  sub_new = __atomic_sub_fetch(&sub_v, 4, 5);
  and_new = __atomic_and_fetch(&and_v, 10, 5);
  or_new = __atomic_or_fetch(&or_v, 1, 5);
  xor_new = __atomic_xor_fetch(&xor_v, 3, 5);

  nand_old = __atomic_fetch_nand(&fn, 10, 5);
  nand_new = __atomic_nand_fetch(&nf, 10, 5);

  __atomic_store(&gdst, &gsrc, 5);
  __atomic_load(&gdst, &gload, 5);
  __atomic_exchange(&gx, &gxval, &gxold, 5);

  int gexpect = 5;
  gcmp_res = __atomic_compare_exchange(&gcmp, &gexpect, &gxval, 0, 5, 5);

  tas_first = __atomic_test_and_set(&flag, 5);
  tas_second = __atomic_test_and_set(&flag, 5);
  __atomic_clear(&flag, 5);
  flag_after_clear = flag;

  // under sequential consistency the fences are no-ops
  __atomic_thread_fence(5);
  __atomic_signal_fence(5);

  __atomic_store_n(&done, 1, 5);

  return 0;
}

void *thr2(void *_) {
  while (__atomic_load_n(&done, 5) == 0)
    ;
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

  // the __atomic_op_fetch builtins yield the updated value
  if (__atomic_load_n(&add_v, 5) != 3 || add_new != 3) reach_error();
  if (__atomic_load_n(&sub_v, 5) != 6 || sub_new != 6) reach_error();
  if (__atomic_load_n(&and_v, 5) != 8 || and_new != 8) reach_error();
  if (__atomic_load_n(&or_v, 5) != 13 || or_new != 13) reach_error();
  if (__atomic_load_n(&xor_v, 5) != 15 || xor_new != 15) reach_error();

  // ~(12 & 10) == ~8 == -9
  if (__atomic_load_n(&fn, 5) != -9 || nand_old != 12) reach_error();
  if (__atomic_load_n(&nf, 5) != -9 || nand_new != -9) reach_error();

  if (__atomic_load_n(&gdst, 5) != 7 || gload != 7) reach_error();
  if (__atomic_load_n(&gx, 5) != 9 || gxold != 1) reach_error();
  if (__atomic_load_n(&gcmp, 5) != 9 || gcmp_res != 1) reach_error();

  if (tas_first != 0 || tas_second != 1) reach_error();
  if (flag_after_clear != 0) reach_error();

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