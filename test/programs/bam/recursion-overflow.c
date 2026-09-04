// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint();

unsigned int id(unsigned int x) {
  if (x == 0) {
    return 0;
  }
  unsigned int ret = id(x - 1) + 1;
  if (ret > 2) {
    return 2;
  }
  return ret;
}

int main(void) {
  unsigned int input = __VERIFIER_nondet_uint();
  unsigned int result = id(input);
  return result == 3;
}
