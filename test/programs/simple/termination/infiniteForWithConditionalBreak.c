// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 CPAchecker contributors
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 0;

  for (;;) {
    if (x >= 2) {
      break;
    }
    x++;
  }

  return 0;
}
