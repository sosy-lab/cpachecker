// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  unsigned int u = 1U;
  signed int s = -1;

  if (((unsigned int)s > u) != 1) goto ERROR;
  if ((((unsigned int)-1) > 1U) != 1) goto ERROR;
  if ((0U < (unsigned int)s) != 1) goto ERROR;

  return 0;
ERROR:
  return 1;
}
