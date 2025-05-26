// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f() {
  int x = 1;
  int y = 1;
  x = 2;
  y = 2;
  while (x != 0 && y != 0) {
    x = x - 1;
    y = y - 1;
  }
  return x + y;
}

int g(int z, int w) { return z + w; }

int main() {
  f();
  g(1, 2);

  int i = 0;
  int j = 0;
  for (i == 0 || j < 0; j == 0 && i < 10; i < 5 && i != 0) {
    i++;
  }

  int q = 0;
  int s = 0;
  int p = s != q ? s == 1 : q == 2;
  int l = (0);

  int t = 1;
  a(1, 1); t = 2;
  int arr[3] = {0, 0, 0};

  g(2/* comment inside the function call */, 2);
  g /* comment between function and argument */ (1, 1);
  arr[ /* comment in element of array */ 0] = 1;
  arr[0] = /* comment in value of array */ 0;

  t = d(1, 2) || g(3, 3);
  if (g(3, 0) == 3) {
    t = 4 + g(2, 3);
  } else if (d(3, 2)) {
    for (int i = 0; i < 3; i++) {
      t += g(0, 1);
    }
  }

  int u = g(a(3, 2), a(2, 3)) * a(4, 1);

  arr[1] = a(1, 4);

  int v = a(
  5,
  6
  );

  arr[2]
  =
  0;
  }

int rec(int x) {
   int p = rec(0);
   p = rec(2);
   int m = rec(/* comment in recursion */ 1);
   int n = rec(a(5, 4));
   int q = rec(rec(3));
   q = rec(rec(4));
   int o = rec(
   rec(
   2
   )
   );
   return rec(x - 2);
}

int a(int v, int w) { if (v > w) { return v - w; } else { return v - 1; } }

int b() {
  int a = 0; int b = 1, c = 3; a = 3; b = 0;
  if ((a > 0 && b == 0) || (c == 3)) {c = 2; int d = a + b;} else {c = 1;}
  do {++b; c--;} while (b < 2 && c > 0);
  c = 4; return b;
}

int c/* testing for comments */ ()  {
  int e = 2; /* comment at end of line */
  /* comment at beginning of line */ int f = 1;
  int /* comment between type and variable */ g = 0;
  if ((e > f) /* comment in conditional */ && (g /* comment in conditional */ == 0)) {
    e /* comment between variable and setting of value */ = 3;
  } else {
    for (int p = 0 /* comment in loop */ ; p /* comment in loop */ < 2; p++ /* comment in loop */) {
    e += /* comment in iteration */ 1;
      }
  }
  return /* comment in return */ f /* comment between operation */ + g;
}

int d(int h, int k) { return (a(h, k) > 0); }

int e(int f, int g) {
  int l = (f, f + 1);
  if (g = 1, g++, g < 3) {
    l = 4, l = 2;
  } else {
    for (int i = 4, j = 5; (i > 1, j > 1); i--, j--) {
      l += (i + 3, j + 1);
    }
  }
  return (f >= 0, g >= 0) && (f < 5, g < 5);
}

int h() {
  int
  p
  =
  10;
  for (
  int q
  =
  1;
  q
  <
  p;
  q++
  )
  {
    p
    =
    p
    -
    q;
  }
    if (
    p
    >
    0
    )
    {
        p
        =
        5;
        }
  return
  p
  +
  3;
}
