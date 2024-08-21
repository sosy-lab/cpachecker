// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int check(int i) {
  if (i == 1) {
    return 0;
  } else {
  ERROR:
    return 0;
  }
}

int main(void) {
  int i = 0;
  int c = 0;

  if (c == 0) {
    i++;
  } else {
    return 0;
  }

  int r = check(i);

  return r;
}
