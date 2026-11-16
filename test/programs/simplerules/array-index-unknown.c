// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Whether the access is inside the array depends on the input, so every rule has to abstain.
extern unsigned char __VERIFIER_nondet_uchar(void);

int array[10];

int main(void) {
  unsigned char index = __VERIFIER_nondet_uchar();
  array[index] = 1;
  return array[0];
}
