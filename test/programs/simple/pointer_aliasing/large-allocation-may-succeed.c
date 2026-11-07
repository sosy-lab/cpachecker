// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *malloc(unsigned int size);

int main() {
  char *a = malloc(0xC0000000u);
  if (a != 0) {
    a[0xBFFFFFFFu] = 1;
    if (a[0xBFFFFFFFu] == 1) {
ERROR:
      return 1;
    }
  }
  return 0;
}
