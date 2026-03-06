// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern unsigned char __VERIFIER_nondet_uchar(void);


int main() {
  unsigned char uchar_var = __VERIFIER_nondet_uchar();

  if (uchar_var <= 150) { // Prevents underflow and restricts uchar_var from 151 to uchar max
    return 0;
  }
  uchar_var--;

  if (uchar_var >= 200) {
    return 0;
  }

  assert(uchar_var != 149); // Safe

  assert(uchar_var != 200); // Safe

  assert(uchar_var != 150); // Fails -> uchar_var == 150

  return 0;
}