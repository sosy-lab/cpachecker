// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;
extern void *malloc(size_t __size);

int main() {
  int *p = malloc(4);
  if (!p) {
ERROR:
    return 1;
  }
  return 0;
}
