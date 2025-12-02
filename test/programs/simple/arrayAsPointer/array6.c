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
  // Check that pointers to arrays are still working
  char msg[] = "Error";
  char (*ptr)[] = &msg;
  (*ptr)[0] = 'C';
  __VERIFIER_assert(msg[0] == 'C');
}
