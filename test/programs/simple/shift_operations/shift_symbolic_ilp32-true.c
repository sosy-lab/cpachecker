// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned long __VERIFIER_nondet_ulong(void);

int main(void) {
  unsigned long x = __VERIFIER_nondet_ulong();
  unsigned long k = __VERIFIER_nondet_ulong();

  if (x == 0xF0F0F0F0UL && k == 4UL) {
    if ((x >> k) != 0x0F0F0F0FUL) {
      goto ERROR;
    }
  }

  unsigned long y = __VERIFIER_nondet_ulong();
  if (y <= 255UL) {
    if ((y >> 8) != 0UL) {
      goto ERROR;
    }
  }

  unsigned long z = __VERIFIER_nondet_ulong();
  if (z <= 65535UL) {
    if ((z << 8) > 0x00FFFF00UL) {
      goto ERROR;
    }
  }

  return 0;

ERROR:
  return 1;
}
