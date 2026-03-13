// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int sum = 0;
  int counter = 0;

  for (int i = 0; i < 4; ++i) {
    sum += 3;
  }

  while (counter < 3) {
    counter++;
  }

  if (sum != 12) goto ERROR;
  if (counter != 3) goto ERROR;

  return 0;
ERROR:
  return 1;
}
