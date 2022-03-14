// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef struct {
  unsigned short a;
} A;

void foo1(unsigned int x) {
  if (x != 0x800 && x != 0x810) {
  ERROR:
    goto ERROR;
  }
}

void foo2(int x, int y) {
  int l;
  if (!((l = (t(x) - t(y)) ? 0 : 1) || 0)) {
    foo1(0x800);
  }
}

int t(int x) { return x + 2; }

int main(int argc, char **argv) {
  int i;

  for (i = 0; i < 2; ++i) {
    foo1(((A){((!(i >> 4) ? 8 : 64 + (i >> 4)) << 8) + (i << 4)}).a);
  }

  int j = 7;
  int a, b;
  foo2(a = 1, b = (j & 2 == 2) ? t(b) : 1);

  exit(0);
}
