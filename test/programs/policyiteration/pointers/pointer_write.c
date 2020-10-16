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
  int a, b, c, undefined;
  int *pointer;
  a = 1;
  b = 10;
  if (undefined) {
    pointer = &a;
  } else {
    pointer = &b;
  }
  *pointer = 42;

  // This assertion should fail, as *pointer may alias to <a>.
  assert(a != 42);
  return 0;
}
