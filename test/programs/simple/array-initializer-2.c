// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f() {
  return 1;
}

int main(void) {
  int a[3] = { 1 };
  int b[3] = { {1}, 2 };
  int c[3] = { f(), f() };
  int d[] = { { 1 }, { { { { 2 } } } }, { { 3 } } };
  int i = { 1 };
  int *p = { { { &a } } };

  if (a[0] != 1) return 0;
  if (a[1] != 0) return 0;
  if (a[1] != 0) return 0;

  if (b[0] != 1) return 0;
  if (b[1] != 2) return 0;
  if (b[2] != 0) return 0;

  if (c[0] != 1) return 0;
  if (c[1] != 1) return 0;
  if (c[2] != 0) return 0;

  if (d[0] != 1) return 0;
  if (d[1] != 2) return 0;
  if (d[2] != 3) return 0;

  if (i != 1) return 0;

  if (p != &a) return 0;

ERROR:
  return 1;
}
