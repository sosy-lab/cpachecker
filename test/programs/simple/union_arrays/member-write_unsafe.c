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
  union u x = {.i = {10, 0}};
  // Writing j must also write the overlapping elements of i.
  // Both halves of j are equal, so i[0] becomes 7 regardless of the byte order.
  x.j = 0x700000007ULL;
  if (x.i[0] == 7) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return 1;
}
