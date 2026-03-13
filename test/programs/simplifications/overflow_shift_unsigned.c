// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  unsigned int x = 1U;
  unsigned int y = x << 3;

  if (y != 8U) goto ERROR;
  if ((y >> 3) != 1U) goto ERROR;

  return 0;
ERROR:
  return 1;
}
