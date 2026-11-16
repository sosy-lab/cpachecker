// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Uses many kinds of ACSL annotations, so that printing and parsing them again can be checked.
// The annotations do not need to hold.

extern int __VERIFIER_nondet_int(void);

int a[10];
int *p;
char c;
double d;

/*@ logic integer square(integer x) = x * x; */
/*@ predicate positive(integer x) = x > 0; */
/*@ predicate bounded(integer x, integer y) = -y <= x && x <= y; */

/*@ requires 0 <= x && x < 100;
    requires \valid(p);
    assigns \nothing;
    ensures \result == square(x);
    ensures \result >= \old(x);
    behavior pos:
      assumes x > 0;
      ensures positive(\result);
    behavior zero:
      assumes x == 0;
      ensures \result == 0;
    complete behaviors pos, zero;
    disjoint behaviors;
*/
int sq(int x) {
  return x * x;
}

/*@ assigns a[0], p; */
void reset(void) {
  a[0] = 0;
}

int main(void) {
  int i = 0;
  int n = __VERIFIER_nondet_int();
  //@ assert c == 'a' || c == '\n' || c == '\'' || c == '\\' || c == '\101' || c == L'\x41';
  //@ assert d >= 1.5 || d < 2. || d == 0x1.8p1 || d == 1e3 || d == 2.50f;
  //@ assert 0x10 == 16 && 010 == 8 && 0b11 == 3 && 42UL == 42 && 0 == 0;
  //@ assert n > 0 ? n >= 1 : n <= 0;
  //@ assert (n > 0 ? n : -n) >= 0;
  //@ assert !(n < 0) ^^ n < 0;
  //@ assert n == 0 <==> !(n != 0);
  //@ assert n * 2 - n / 3 + n % 4 == (n << 1) - (n >> 1) + (n & 1) + (n | 1) + (n ^ 1);
  //@ assert n == n --> n == n;
  //@ assert bounded(n, n * n + 1) || \true || \false;
  //@ assert n;
  //@ assert positive(n) ==> square(n) > 0;
  /*@ loop invariant 0 <= i <= 10;
      loop invariant \forall integer k; 0 <= k < i ==> a[k] == k;
      loop invariant \exists integer k, integer l; k == i && l == k;
      loop assigns i, a[0];
  */
  while (i < 10) {
    a[i] = i;
    i++;
  }
  //@ assert \at(i, Here) == 10 && \at(i, Pre) <= 10;
  /*@ requires i == 10;
      ensures i == 11;
  */
  i++;
  reset();
  return sq(i % 100);
}
