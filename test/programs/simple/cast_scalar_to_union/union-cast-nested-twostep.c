// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef union Inner {
  float f;
  int i;
} Inner;

typedef union Outer {
  double d;
  Inner in;
} Outer;

int main(void) {
  int x = 7;

  Inner a = (Inner) x;   // must choose i
  Outer o = (Outer) a;  // must choose in

  if (o.in.i != 7) goto error;
  return 0;

error:
  return -1;
}
