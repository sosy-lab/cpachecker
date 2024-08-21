// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  short int x = 0;
  short int y = 1;
  if ((x | y) == 0) {
    return 1;
  } else {
ERROR:
    return 0;
  }
}
