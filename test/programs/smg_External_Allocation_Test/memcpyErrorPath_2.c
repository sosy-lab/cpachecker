// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  char *a;
  char b[3] = "abc";
  a = malloc(4);
  int i = __VERIFIER_nondet_int();
  if (i <= 4) {
    memcpy(a, b, i);
  }
  free(a);
  return 0;
}
