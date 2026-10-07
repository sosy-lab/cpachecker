// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The result of the addition is outside the range of int, and every execution computes it.
int main(void) {
  int x = 2147483647 + 1;
  return x;
}
