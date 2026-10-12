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

int xor_v = 12;
int xor_new = 0;
int done = 0;

void *thr1(void *_) {
  xor_new = __atomic_xor_fetch(&xor_v, 3, 5);
  __atomic_store_n(&done, 1, 5);
  return 0;
}

void *thr2(void *_) {
  while (__atomic_load_n(&done, 5) == 0)
    ;
  if (__atomic_load_n(&xor_v, 5) != 15 || xor_new != 12) reach_error();
  return 0;
}

int main() {
  thr1(0);
  thr2(0);
  return 0;
}
