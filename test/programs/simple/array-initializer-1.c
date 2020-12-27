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

  if (a[0] != 1) goto ERROR;
  if (a[1] != 0) goto ERROR;
  if (a[1] != 0) goto ERROR;

  if (b[0] != 1) goto ERROR;
  if (b[1] != 2) goto ERROR;
  if (b[2] != 0) goto ERROR;

  if (c[0] != 1) goto ERROR;
  if (c[1] != 1) goto ERROR;
  if (c[2] != 0) goto ERROR;

  if (d[0] != 1) goto ERROR;
  if (d[1] != 2) goto ERROR;
  if (d[2] != 3) goto ERROR;

  if (i != 1) goto ERROR;

  if (p != &a) goto ERROR;

  return 0;
ERROR:
  return 1;
}
