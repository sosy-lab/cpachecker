// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  unsigned int x = 0x2Au;

  if ((x & 0u) != 0u) goto ERROR;
  if ((x | 0u) != x) goto ERROR;
  if ((x ^ x) != 0u) goto ERROR;
  if ((x & 0xFFu) != x) goto ERROR;

  return 0;
ERROR:
  return 1;
}
