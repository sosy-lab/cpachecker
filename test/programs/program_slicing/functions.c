// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void relevant1(int *i) { *i = (*i) + 1; }

int relevant2() { return 1; }

void irrelevant1(int *i) { int j = 0; }

int irrelevant2() { return 0; }

int checkEqualsOne(int i) {
  if (i == 1) {
    return 0;
  } else {
  ERROR:
    return 1;
  }
}

int main(void) {
  int i = 0;

  int x;
  int y;

  relevant1(&i);
  x = relevant2();

  checkEqualsOne(x);

  irrelevant1(&i);
  y = irrelevant2();

  return checkEqualsOne(i);
}
