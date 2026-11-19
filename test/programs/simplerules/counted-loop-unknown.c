// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The loop of this program ends after ten iterations, but no trivial rule argues about how
// often a loop runs, so the rules have to abstain.
int main(void) {
  int sum = 0;
  for (int i = 0; i < 10; i++) {
    sum = sum + i;
  }
  return sum;
}
