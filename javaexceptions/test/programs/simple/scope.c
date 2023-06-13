// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void foo(int param) {
    double dou;
    for (param=0; param<5;param++) {
        int intern = 0;
    }
    dou = param;
}

int foo2(int param) {
    double dou;
    for (param=0; param<5;param++) {
        int intern = 0;
        if (intern) break;
    }
    dou = 5;
    return param;
}

int foo3(int param) {
    double dou;
    for (param=0; param<5;param++) {
        int intern = 0;
        if (intern) return intern;
    }
    dou = 5;
    return param;
}

int foo4(int param) {
    double dou = 5;
    return dou;
}

void foo5(int param) {
    double dou = 5;
}

int main() {
  {
  int a = 5;
  foo(a);
  foo2(a);
  if (a==0) {
    int b = 3;
  } else {
    int c = 1;
    {{{{ int d = 5; }}}}
    c++;
  }
  foo3(a);
  foo4(a);
  int b;
  switch (b) {
      case 3:
      return b;
      case a:
      break;
      default:
      return a;
  }
  foo5(a);
  }
  int test = 4;
  while (test++) {
    int xx = 0;
    int test2 = 4;
    while (test2++) {
      int xx2 = 0;
    }
  }
  int zz;
  return 0;
}
