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

struct inner {
  char data[4];
};

struct outer {
  int tag;
  struct inner in;
};

int main() {
  struct outer o;
  o.in.data[2] = 'X';
  // The array field is nested two levels deep (outer struct -> inner struct -> array), to check
  // whether the same handling applies recursively through nested composite types.
  memset(&o, 0, sizeof(o));
  if (o.in.data[2] == 'X') {
    // not reachable: memset zeroes the whole struct, including the nested data[2].
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return -1;
}
