// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int a[2];
  int *p = &a[0];

  if ((p == 0) != 0) goto ERROR;
  if ((p != 0) != 1) goto ERROR;
  if ((p + 0) != p) goto ERROR;
  if ((&a[0] == a) != 1) goto ERROR;
  if (((p + 1) - p) != 1) goto ERROR;

  return 0;
ERROR:
  return 1;
}
