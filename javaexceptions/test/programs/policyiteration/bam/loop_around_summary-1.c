// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }

const int BOUND = 10;

int inc(int input) {
  return input + 1;
}

int main() {
  int sum = 0;
  int i;
  for (i=0; i<BOUND; i++) {
    sum = inc(sum);
  }
  assert(sum == BOUND);
  assert(sum == i);
  return 0;
}
