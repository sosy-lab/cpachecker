// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


int main() {
  int y = 2;
  int x = 2;
  while (x != 0 && y != 0) {
    x = x - 1;
  }
  return x + y;
}
