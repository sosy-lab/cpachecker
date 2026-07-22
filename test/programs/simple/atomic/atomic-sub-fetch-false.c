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

int sub_v = 10;
int sub_new = 0;
int done = 0;

void *thr1(void *_) {
  sub_new = __atomic_sub_fetch(&sub_v, 4, 5);
  __atomic_store_n(&done, 1, 5);
  return 0;
}

void *thr2(void *_) {
  while (__atomic_load_n(&done, 5) == 0)
    ;
  if (__atomic_load_n(&sub_v, 5) != 6 || sub_new != 10) reach_error();
  return 0;
}

int main() {
  thr1(0);
  thr2(0);
  return 0;
}
