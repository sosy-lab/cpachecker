// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int repeated_addition = 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1;
  int mixed_arithmetic = (7 * 6) - (5 + 4);
  int nested_arithmetic = (3 * (4 + 2)) / 2;

  if (repeated_addition != 8) goto ERROR;
  if (mixed_arithmetic != 33) goto ERROR;
  if (nested_arithmetic != 9) goto ERROR;

  return 0;
ERROR:
  return 1;
}
