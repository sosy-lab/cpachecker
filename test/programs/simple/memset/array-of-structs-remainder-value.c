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
  arr[1].data[2] = 0;
  // Same shape as array-of-structs-remainder.c, but checks that the *new* value set by memset
  // actually shows up in the ceiling-rounded trailing element's array field, instead of only
  // checking that a stale value is gone.
  memset(arr, 'B', 9);
  if (arr[1].data[2] == 'B') {
    // reachable: ceiling-rounding covers all of arr[1], setting data[2] to 'B'.
    reach_error();
  }
}
