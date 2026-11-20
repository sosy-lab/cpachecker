// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Every execution reaches the error: nothing in front of it can branch.
extern void reach_error(void);

void fail(void) { reach_error(); }

int main(void) {
  int x = 3;
  int y = x + 4;
  fail();
  return y;
}
