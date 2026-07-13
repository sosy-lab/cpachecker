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
  __assert_fail("0", "example-atomic-fetch-add-false.c", 15, "reach_error");
}

int d = 0;
int d_out = 0;
int done = 0;

void *thr1(void *_) {
  int oldd = __atomic_fetch_add(&d, 5, 5);
  __atomic_store_n(&d_out, oldd, 5);
  __atomic_store_n(&done, 1, 5);
  return 0;
}

void *thr2(void *_) {
  while (__atomic_load_n(&done, 5) == 0)
    ;
  if (__atomic_load_n(&d, 5) != 5 || __atomic_load_n(&d_out, 5) != 5) reach_error();
  return 0;
}

int main() {
  thr1(0);
  thr2(0);
  return 0;
}
