// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// unused inside expression list with side effects

int main() {
  int a = 5;
  (
    4,
    ({a++; int c = ++a;})
  );

  if(a != 7) {
    ERROR: // unreachable
      return 1;
  }

  return 0;
}
