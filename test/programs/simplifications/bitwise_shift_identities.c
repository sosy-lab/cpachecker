// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  unsigned int x = 21u;

  if ((x | x) != x) goto ERROR;
  if ((x & x) != x) goto ERROR;
  if ((x ^ 0u) != x) goto ERROR;
  if ((x << 0) != x) goto ERROR;
  if ((x >> 0) != x) goto ERROR;
  if (((x << 2) >> 2) != x) goto ERROR;

  return 0;
ERROR:
  return 1;
}
