// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;
extern void *alloca (size_t __size);

int main() {
  int *p = alloca(sizeof(int));
  if (p == 0) {
    goto ERROR;  // alloca never returns null
  }
  *p = 42;
  if (*p != 42) {
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
