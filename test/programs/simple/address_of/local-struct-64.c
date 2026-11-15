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
  int arr[4];
};

// Machine model: LP64. Expected: true for valid-memsafety.
// &s.f is the address of the struct s on the stack plus the offset of f.
int main(void) {
  struct outer s = { 1, { 2, 3 }, { 0, 0, 0, 0 } };

  int *n = &s.n;
  check(n == (int *)&s);
  *n = 4;
  check(s.n == 4);

  struct inner *in = &s.in;
  int *b = &s.in.b;
  check(b == &in->b);
  *b = 5;
  check(s.in.b == 5);
  check(s.in.a == 2);

  int *element = &s.arr[3];
  check(element == s.arr + 3);
  *element = 6;
  check(s.arr[3] == 6);

  return 0;
}
