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

void count() {
  if (g < max) {
    g++;
    int a = 0;
    count();
    a += 1;
    if (a == 1) reach_error();
  }
}


int main() {
  count();
  if (g != max) {
    reach_error();
  }
}