// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f(int y) {
  return y;
}

int main() {
  int x[2];
  for (int i = 0; i < 2; i++) {
    x[i] = i;
  }
  int r = f(x[0]);
  for (int i = 0; i < 2; i++) {
    r = r + x[i];
  }
  return r;
}
