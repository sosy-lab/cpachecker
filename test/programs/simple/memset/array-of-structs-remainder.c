// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

void reach_error() {}
extern void *memset(void *s, int c, size_t n);

struct rec {
  int tag;
  char data[4];
};

int main() {
  struct rec arr[2];
  arr[1].data[2] = 'X';
  // sizeof(struct rec) == 8 (int tag = 4 bytes + char data[4] = 4 bytes).
  // 9 bytes = 1 full element (arr[0]) plus 1 remainder byte into arr[1].tag. Since the
  // remainder does not divide the element size, this ceiling-rounds up to fully cover the
  // *entire* trailing element arr[1], including its array field arr[1].data.
  memset(arr, 0, 9);
  if (arr[1].data[2] == 'X') {
    // not reachable: ceiling-rounding covers all of arr[1], zeroing data[2].
    reach_error();
  }
}
