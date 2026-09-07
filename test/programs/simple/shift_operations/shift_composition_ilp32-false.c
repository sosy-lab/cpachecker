// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  /* Equivalent shift sequences should yield identical values. */
  unsigned int x = 0x00012345U;
  if (((x << 4) << 8) != (x << 12)) {
    goto ERROR;
  }
  if (((x >> 4) >> 8) != ((x >> 12) + 1U)) {
    goto ERROR;
  }

  x = 0x00123456U;
  if (((x << 8) >> 8) != x) {
    goto ERROR;
  }

  unsigned long lx = 0x00012345UL;
  if (((lx << 4) << 8) != (lx << 12)) {
    goto ERROR;
  }

  unsigned long long llx = 0x0000000123456789ULL;
  if (((llx << 8) >> 8) != llx) {
    goto ERROR;
  }
  if (((llx >> 7) >> 9) != (llx >> 16)) {
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
