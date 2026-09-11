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
  r.data[2] = 'X';
  // A single, exactly-sized memset of the whole struct. No remainder/ceiling-rounding is
  // involved here: this only tests whether an array field nested inside a memset'd struct is
  // zeroed at all.
  memset(&r, 0, sizeof(r));
  if (r.data[2] == 'X') {
    // not reachable: memset zeroed data[2], overwriting 'X'.
    reach_error();
  }
}
