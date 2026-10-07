// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The only loop of the program is behind a condition that is never true, so no execution
// reaches it. A rule that only asks whether the program has a loop would have to abstain.
int enabled = 0;

int main(void) {
  if (enabled) {
    while (1) {
    }
  }
  return 0;
}
