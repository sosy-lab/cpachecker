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
  unsigned char b[3] = {0, 0, 0};
  __VERIFIER_nondet_memory(&b[0], sizeof(b));
  // some '+' operation for no-overflow property:
  // every b[i] is in [0, 255], so sum is at most 765 and cannot overflow
  int sum = b[0] + b[1] + b[2];
  // reachable with b == {1, 2, 3}
  if (b[0] == 1 && b[1] == 2 && sum == 6) {
    reach_error();
  }
}
