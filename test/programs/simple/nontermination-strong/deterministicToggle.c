// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 CPAchecker contributors
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 1;
  while (x > 0) {
    if (x == 1) {
      x = 2;
    } else {
      x = 1;
    }
  }
  return 0;
}
