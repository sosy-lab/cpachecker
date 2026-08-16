// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint();

int main() {
  unsigned int i, n = __VERIFIER_nondet_uint();
  unsigned long long sn = 0;
  // n needs to be less than 2^32 such that the multiplication part
  // of the Gauss sum does not exceed the range of unsigned long long
  if (n >= 4294967296U)
    goto EXIT;

  for (i = 0; i <= n; i++) {
    sn = sn + i;
  }
  // Compute Gauss sum without overflow
  unsigned long long gauss = (n * (n + 1U)) / 2U;
  if (sn == gauss || sn == 0) {
    goto EXIT;
  } else {
  ERROR:
    return 1;
  }
EXIT:
  return 0;
}
