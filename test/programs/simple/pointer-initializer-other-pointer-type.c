// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A pointer is initialized with a pointer to another type, which compilers accept with a warning.
struct s {
  int len;
  const unsigned char bytes[4];
};
struct s g;

int main(void) {
  char *a = g.bytes;
  if (a != (char *)g.bytes) {
  ERROR:
    return 1;
  }
  return 0;
}
