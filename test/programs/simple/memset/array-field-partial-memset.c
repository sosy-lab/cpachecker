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

struct msg {
  int id;
  char payload[12];
};

int main() {
  struct msg m;
  for (int i = 0; i < 12; i++) {
    m.payload[i] = 0;
  }
  m.payload[11] = 9;
  // The destination is the array field itself (not a struct that contains it), so the element
  // type being iterated is plain 'char', not an array type. This byte-precisely sets only the
  // first 5 bytes of payload; payload[11] must stay untouched.
  memset(m.payload, 'A', 5);
  if (m.payload[11] != 9) {
    // not reachable: memset only touches payload[0..4], payload[11] stays 9.
    reach_error();
  }
}
