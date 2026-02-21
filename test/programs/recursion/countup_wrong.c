// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error();

int g = 0;
int max = 10;

void count() {
  if (g < max) {
    g++;
    count();
  }
}

int main() {
  count();
  if (g == max) {
    reach_error();
  }
}