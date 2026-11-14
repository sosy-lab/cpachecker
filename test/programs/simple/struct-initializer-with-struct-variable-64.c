// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Dereferences a null pointer if the condition does not hold.
void check(int condition) {
  if (!condition) {
    int *null = 0;
    *null = 0;
  }
}

struct inner {
  int a;
  int b;
};

struct outer {
  int n;
  struct inner in;
  int m;
};

// Machine model: LP64. Expected: true for valid-memsafety.
// A struct variable inside an initializer list initializes only its member.
int main(void) {
  struct inner first = { 5, 6 };

  struct outer s = { 1, first, 7 };
  check(s.n == 1);
  check(s.in.a == 5);
  check(s.in.b == 6);
  check(s.m == 7);

  struct inner pair[2] = { { 3, 4 }, first };
  check(pair[0].a == 3);
  check(pair[1].b == 6);

  return 0;
}
