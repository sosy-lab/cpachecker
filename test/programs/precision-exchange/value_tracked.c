// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error(void);

int main() {
  int i = 0;
  int sum = 0;

  while (i < 10) {
    sum = sum + 2;
    i++;
  }

  if (sum != 20) reach_error();

  return 0;
}
