// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int square(int x) {
  return x * x;
}

int main() {
  int i = 0;
  int sum = 0;

  while (1) {
    if (i == 4) {
      goto LOOPEND;
    } else {
      sum = sum + square(i);
      i = i + 1;
    }

    if (sum < 0) {
      goto ERROR;
    }
  }

  LOOPEND:

  if (sum != 14) {
    goto ERROR;
  }

  return sum;

  ERROR:
  return -1;
}
