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

int and_v = 12;
int and_new = 0;
int done = 0;

void *thr1(void *_) {
  and_new = __atomic_and_fetch(&and_v, 10, 5);
  __atomic_store_n(&done, 1, 5);
  return 0;
}

void *thr2(void *_) {
  while (__atomic_load_n(&done, 5) == 0)
    ;
  if (__atomic_load_n(&and_v, 5) != 8 || and_new != 12) reach_error();
  return 0;
}

int main() {
  thr1(0);
  thr2(0);
  return 0;
}
