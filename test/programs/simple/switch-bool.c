// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern _Bool __VERIFIER_nondet_bool();

int main() {
  _Bool b = __VERIFIER_nondet_bool();
  switch (b) {
    case 0: return 0;
    case 1: return 0;
  }
ERROR:
  return 1;
}
