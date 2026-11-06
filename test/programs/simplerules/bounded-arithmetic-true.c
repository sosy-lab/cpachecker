// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The arithmetic of this program cannot overflow: the operands of the signed operations are
// narrow enough for every result to fit into an int, and the unsigned arithmetic may wrap
// around, which the property allows.
extern unsigned char __VERIFIER_nondet_uchar(void);
extern unsigned int __VERIFIER_nondet_uint(void);

int main(void) {
  unsigned char a = __VERIFIER_nondet_uchar();
  unsigned char b = __VERIFIER_nondet_uchar();
  if (a * b + 1 > 0) {
    return 1;
  }
  unsigned int c = __VERIFIER_nondet_uint();
  unsigned int d = c * 3 + 7;
  return (int)(d & 1);
}
