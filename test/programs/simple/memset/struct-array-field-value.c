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
  struct rec r;
  r.data[2] = 0;
  // Same shape as struct-array-field.c, but checks that the *new* value set by memset actually
  // shows up in the array field, instead of only checking that a stale value is gone: if the
  // array field assignment is silently dropped, its old value (0) would incorrectly persist.
  memset(&r, 'B', sizeof(r));
  if (r.data[2] == 'B') {
    // reachable: memset sets every byte of r, including data[2], to 'B'.
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return -1;
}
