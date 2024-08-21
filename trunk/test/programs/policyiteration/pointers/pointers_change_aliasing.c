// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
  int a = 10;
  int b = 100;
  int *p;
  p = &a;
  for (int i=0; i<10; i++) {
    p = &b;
  }
  *p = 20;
  assert(a == 20);

}
