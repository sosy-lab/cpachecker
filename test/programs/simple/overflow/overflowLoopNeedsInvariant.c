// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// No overflow, but proving it needs the loop invariant i <= 1000000: the negated
// loop condition alone gives no upper bound on i, so plain k-induction needs an
// induction depth of 1000000, while combining it with a data-flow-supplied
// invariant proves it directly.
int main() {
  int i = 0;
  while (i < 1000000) {
    i = i + 1;
  }
  int big = i * 2000;
  return big;
}
