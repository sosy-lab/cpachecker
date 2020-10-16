// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  short int x = 1;
  long long int y = 0;
  if ((x | y) == 0) {
ERROR:
    return 1;
  } else {
    return 0;
  }
}
