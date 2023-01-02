// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error();

int main() {
  int r = 0;
  int a = 10;
  if (a < 0) {
    while (a < 0) {
      r = r - a;
      a = a + 1;
    }
  } else {
    r = a * (a + 1); // modification
    r = r / 2;
  }

  if (r != 55) {
    reach_error();
  }
}
