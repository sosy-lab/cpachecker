// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error();

int g = 0;
int max = 3;

void count(int *c, int offset) {
  if (*(c - offset) < max) {
    (*(c - offset))++;
    int** a = &c;
    count(*a + 1, offset + 1);
    (**a) += 1;
  }
}


int main() {
  count(&g, 0);
  if (g == 2*max) {
    ERROR: reach_error();
  }
}