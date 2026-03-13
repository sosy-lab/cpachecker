// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 9;

  if ((x == x) != 1) goto ERROR;
  if ((x != x) != 0) goto ERROR;
  if ((x < x) != 0) goto ERROR;
  if ((x <= x) != 1) goto ERROR;
  if ((x > x) != 0) goto ERROR;
  if ((x >= x) != 1) goto ERROR;

  if ((3 < 4) != 1) goto ERROR;
  if ((4 <= 4) != 1) goto ERROR;
  if ((5 > 7) != 0) goto ERROR;
  if ((7 >= 8) != 0) goto ERROR;

  return 0;
ERROR:
  return 1;
}
