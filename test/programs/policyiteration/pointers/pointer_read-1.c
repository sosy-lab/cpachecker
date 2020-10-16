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
  c = *pointer;

  // Convex hull of two points.
  assert(c >= 1 && c <= 10);
}
