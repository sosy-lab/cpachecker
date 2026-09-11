// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

void reach_error() {}
extern void *memcpy(void *dest, const void *src, size_t n);

struct rec {
  int tag;
  char data[4];
};

int main() {
  struct rec src;
  struct rec dst;
  src.tag = 0;
  src.data[0] = 0;
  src.data[1] = 0;
  src.data[2] = 0;
  src.data[3] = 0;
  dst.data[2] = 'X';
  // Unlike memset (which assigns a repeated scalar byte, of a different type than the array
  // field it may land on), memcpy assigns between two same-typed 'struct rec' objects, so the
  // array field is copied field-to-field with matching types on both sides.
  memcpy(&dst, &src, sizeof(struct rec));
  if (dst.data[2] == 'X') {
    // not reachable: memcpy overwrote dst.data[2] with src.data[2] (0), not 'X'.
    reach_error();
  }
}
