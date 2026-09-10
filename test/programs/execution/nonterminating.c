// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This program does not terminate, so executing it does not terminate either and the
// analysis can only run into its time limit. The result is UNKNOWN, never FALSE.
int main(void) {
  int i = 0;
  while (1) {
    i = (i + 1) % 7;
  }
  return 0;
}
