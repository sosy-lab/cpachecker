// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// h is only called directly, and its result is discarded.

int h(int x) { return x + 1; }

int main() {
  h(1);
  return 0;
}
