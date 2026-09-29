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
  unsigned char m[2][3] = {{0, 0, 0}, {4, 5, 6}};
  // The region given to nondet_memory covers the first 3 of the 6
  // elements, so the second row keeps its values.
  // => if-branch is not reachable.
  __VERIFIER_nondet_memory(&m, sizeof(m[0]));
  if (m[1][0] != 4 || m[1][1] != 5 || m[1][2] != 6) {
    reach_error();
  }
}
