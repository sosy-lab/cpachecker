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

struct first {
  struct inner in;
  int n;
};

struct middle {
  int n;
  struct inner in;
  int m;
};

struct last {
  long n;
  char c;
  struct inner in;
};

struct nested {
  int n;
  struct middle mid;
};

union variant {
  struct inner in;
  long long values[4];
};

struct pointers {
  int *p;
  int *q;
};

struct holder {
  int n;
  struct pointers ptrs;
};

// Machine model: LP64. Expected: true for valid-memsafety.
// A struct variable inside an initializer list initializes only its member, at the offset of the
// member, and the other members keep their values.
int main(void) {
  struct inner value = { 5, 6 };

  // The variable as first, middle, and last member.
  struct first f = { value, 7 };
  check(f.in.a == 5);
  check(f.in.b == 6);
  check(f.n == 7);

  struct middle m = { 1, value, 7 };
  check(m.n == 1);
  check(m.in.a == 5);
  check(m.in.b == 6);
  check(m.m == 7);

  struct last l = { 1L, 'c', value };
  check(l.n == 1L);
  check(l.c == 'c');
  check(l.in.a == 5);
  check(l.in.b == 6);

  // Designated initializer in a different order than the members.
  struct middle d = { .m = 7, .in = value, .n = 1 };
  check(d.n == 1);
  check(d.in.b == 6);
  check(d.m == 7);

  // The variable in a nested initializer list.
  struct nested n = { 2, { 1, value, 7 } };
  check(n.n == 2);
  check(n.mid.n == 1);
  check(n.mid.in.a == 5);
  check(n.mid.m == 7);

  // The variable initializes a union, which is larger than the struct.
  union variant v = { value };
  check(v.in.a == 5);
  check(v.in.b == 6);

  // The variable as element in the middle of an array of structs.
  struct inner array[3] = { { 1, 2 }, value, { 3, 4 } };
  check(array[0].b == 2);
  check(array[1].a == 5);
  check(array[1].b == 6);
  check(array[2].a == 3);

  // The copied pointers stay valid and point to the same objects.
  int x = 3;
  int y = 4;
  struct pointers ptrs = { &x, &y };
  struct holder h = { 1, ptrs };
  check(h.n == 1);
  check(*h.ptrs.p == 3);
  check(*h.ptrs.q == 4);

  return 0;
}
