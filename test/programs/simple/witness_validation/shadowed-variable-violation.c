// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
void reach_error() {}

int main() {
  int data = 0;
  {
    int data = __VERIFIER_nondet_int();
    if (data == 5) {
      reach_error();
    }
  }
  return data;
}
