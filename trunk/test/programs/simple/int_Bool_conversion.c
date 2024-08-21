// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct s {
  _Bool b : 1;
};

int main (void) {
  struct s s;
  s.b = 2;
  _Bool b = 2;
  if (s.b != 1 || b != 1) {
    return 0;
  }
ERROR:
  return 1;
}
