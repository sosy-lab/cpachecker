// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f(int n) {
  int result = 0;
  {
    int n = 2;
    result = result + n;
  }
  return result + n;
}

int main() {
  return f(1);
}
