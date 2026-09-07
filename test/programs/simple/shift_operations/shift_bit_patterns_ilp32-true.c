// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  /* Nontrivial bit patterns exercise bit movement instead of only 0, 1, and extrema. */
  if ((0xAAAAAAAAU >> 1) != 0x55555555U) {
    goto ERROR;
  }
  if ((0xF0F0F0F0U >> 4) != 0x0F0F0F0FU) {
    goto ERROR;
  }
  if ((0x00FF00FFU << 8) != 0xFF00FF00U) {
    goto ERROR;
  }
  if ((0x80000000U >> 31) != 1U) {
    goto ERROR;
  }
  if ((0xF0F0F0F0UL >> 4) != 0x0F0F0F0FUL) {  /* 32-bit unsigned long right shift */
    goto ERROR;
  }
  if ((0x0000FFFFUL << 16) != 0xFFFF0000UL) {  /* 32-bit unsigned long left shift without lost bits */
    goto ERROR;
  }
  if ((0xAAAAAAAAAAAAAAAAULL >> 1) != 0x5555555555555555ULL) {
    goto ERROR;
  }
  if ((0x00FF00FF00FF00FFULL << 8) != 0xFF00FF00FF00FF00ULL) {
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
