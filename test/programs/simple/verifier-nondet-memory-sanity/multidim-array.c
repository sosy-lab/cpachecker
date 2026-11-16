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
  unsigned char m[2][3] = {{0, 0, 0}, {0, 0, 0}};
  // All values of m are set non-deterministically, so the if-branch is reachable.
  __VERIFIER_nondet_memory(&m, sizeof(m));
  if (m[0][0] == 1 && m[0][2] == 2 && m[1][0] == 3 && m[1][2] == 4) {
    reach_error();
  }
}
