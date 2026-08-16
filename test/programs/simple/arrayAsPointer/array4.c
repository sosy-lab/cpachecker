// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);

void __VERIFIER_assert(int cond) {
  if (!cond) {
  ERROR: abort();
  }
}

int main() {
  // Do some pointer arithmetics on an array
  char msg[] = "Error";
  __VERIFIER_assert(*(msg+1) == 'r');
}
