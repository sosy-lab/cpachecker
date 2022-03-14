// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void* malloc(unsigned long);
extern void __VERIFIER_error();

int main() {
  char a[2];
  char b;

  if (((long)&b) % 4 == 0) {
    goto EXIT;
  }

  if (((long)&a) % 4 == 0) {
    goto EXIT;
  }

ERROR:
  return 1;
EXIT:
  return 0;
}
