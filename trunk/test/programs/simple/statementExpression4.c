// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// inside expression list, value used

int main() {
  int i = (4, ({int c; c = 42;}) );

  if(i != 42) {
    ERROR: // unreachable
      return 1;
  }

  return 0;
}
