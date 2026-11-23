// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Every execution reaches the loop, and the loop has no exit, so no execution ends.
int main(void) {
  int i = 0;
  while (1) {
    i = i + 1;
  }
  return i;
}
