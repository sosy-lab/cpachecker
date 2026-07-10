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
  __assert_fail("0", "example-atomic-pointer-arithmetic.c", 14, "reach_error");
}

int arr[5] = {0, 0, 0, 0, 0};

int main() {
  int i = 2;
  // the pointer argument need not be a bare "&x": array subscripts and
  // pointer arithmetic must resolve to the same target as well.
  __atomic_store_n(&arr[i], 7, 5);
  __atomic_fetch_add(arr + i, 1, 5);
  __atomic_store_n(&arr[i + 1], 3, 5);

  if (__atomic_load_n(&arr[2], 5) != 8) reach_error();
  if (__atomic_load_n(arr + 3, 5) != 3) reach_error();
  return 0;
}
