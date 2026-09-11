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
  // Same shape as array-of-structs-remainder.c, but sets a nonzero byte value and checks it does
  // NOT leak past the requested 9-byte range into arr[1].data.
  memset(arr, 'B', 9);
  if (arr[1].data[2] == 'B') {
    // not reachable: the memset never reaches arr[1].data, so it stays 0.
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return -1;
}
