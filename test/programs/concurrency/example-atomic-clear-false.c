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
  __assert_fail("0", "example-atomic-clear-false.c", 15, "reach_error");
}

char flag = 1;
int flag_after_clear = 1;
int done = 0;

void *thr1(void *_) {
  __atomic_clear(&flag, 5);
  flag_after_clear = flag;
  __atomic_store_n(&done, 1, 5);
  return 0;
}

void *thr2(void *_) {
  while (__atomic_load_n(&done, 5) == 0)
    ;
  if (flag_after_clear != 1) reach_error();
  return 0;
}

int main() {
  thr1(0);
  thr2(0);
  return 0;
}
