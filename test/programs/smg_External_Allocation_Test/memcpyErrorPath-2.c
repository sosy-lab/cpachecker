// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  char *a;
  char b[] = "ab";
  a = malloc(3);
  int i = __VERIFIER_nondet_int();
  if (i <= 3) {
    memcpy(a, b, i);
  }
  free(a);
  return 0;
}
