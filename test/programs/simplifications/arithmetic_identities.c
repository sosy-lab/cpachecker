// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 13;

  if (x + 0 != 13) goto ERROR;
  if (x - 0 != 13) goto ERROR;
  if (x * 1 != 13) goto ERROR;
  if (x / 1 != 13) goto ERROR;
  if (x % 1 != 0) goto ERROR;
  if (0 - x != -13) goto ERROR;

  return 0;
ERROR:
  return 1;
}
