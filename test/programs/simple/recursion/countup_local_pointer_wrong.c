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

void count(int *c) {
  if (*c < max) {
    *c++;
    int* a = c;
    count();
    a += 1;
  }
}


int main() {
  count(&g);
  if (g == 2*max) {
    reach_error();
  }
}