// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

void reach_error() {}
extern void __VERIFIER_nondet_memory(void *ptr, size_t size);

int main() {
  int arr[2] = {0, 0};
  // 5 bytes = 1 full int (arr[0]) + 1 remainder byte, the low byte of arr[1].
  __VERIFIER_nondet_memory(&arr[0], 5);
  if (arr[1] > 255) {
    reach_error();
  }
}
