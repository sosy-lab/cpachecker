// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef union {
  unsigned long x[4];
} map_word;

map_word build(void) {
  map_word w;
  w.x[0] = 42;
  return w;
}

int main(void) {
  map_word tmp = build();
  map_word w = (map_word)tmp; // cast to the own type is a no-op

  if (w.x[0] != 42) goto error;
  return 0;

error:
  return -1;
}
