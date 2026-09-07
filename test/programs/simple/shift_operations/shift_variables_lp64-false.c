// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  /* Values and counts are variables so the verifier cannot rely only on literal folding. */
  unsigned int x = 0xAAAAAAAAU;
  unsigned int k = 17U;
  if ((x >> k) != 0x00005554U) {
    goto ERROR;
  }

  x = 0x00012345U;
  k = 12U;
  if ((x << k) != 0x12345000U) {
    goto ERROR;
  }

  const unsigned char narrow_k = 8;
  x = 0xF0000000U;
  if ((x >> narrow_k) != 0x00F00000U) {
    goto ERROR;
  }

  unsigned long lx = 0xAAAAAAAAAAAAAAAAUL;
  unsigned long lk = 33UL;
  if ((lx >> lk) != 0x0000000055555555UL) {
    goto ERROR;
  }

  unsigned long long llx = 0xAAAAAAAAAAAAAAAAULL;
  unsigned int llk = 33U;
  if ((llx >> llk) != 0x0000000055555555ULL) {
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
