// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

union u {
  unsigned int i[2];
  unsigned long long j;
};

int main() {
  // Writing the elements of i must also write the overlapping member j, both in initializers
  // and in assignments. Both halves of j are equal, so j does not depend on the byte order.
  union u x = {.i = {1, 1}};
  unsigned long long initialized = x.j;
  x.i[0] = 3;
  x.i[1] = 3;
  if (initialized != 0x100000001ULL || x.j != 0x300000003ULL) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
