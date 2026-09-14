// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int abs(int);
extern long long llabs(long long);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  // Converted to the signed parameter type, 0xFFFFFFFF is -1 and so is 0xFF...FF.
  unsigned int ui = 4294967295u;
  unsigned long long ull = 18446744073709551615ULL;

  __VERIFIER_assert(abs(ui) != 1);
  __VERIFIER_assert(llabs(ull) != 1);

  return 0;
}
