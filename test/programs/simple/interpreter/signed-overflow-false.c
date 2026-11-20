// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  int x = 2147483600;
  for (int i = 0; i < 100; i++) {
    x = x + 1; // overflows after 47 iterations
  }
  return x;
}
