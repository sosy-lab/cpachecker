// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

unsigned char __VERIFIER_nondet_uchar(void);
void __VERIFIER_error();
void __VERIFIER_assume(int cond);

int main() {
  int x = __VERIFIER_nondet_uchar();
  if (x > 0) {
    goto return_label;
  } else {
    x = x + 1;
  }
  if (x > 0) {
    __VERIFIER_error();
  } else {
    goto return_label;
  }


  return_label:
  return 0;
}
