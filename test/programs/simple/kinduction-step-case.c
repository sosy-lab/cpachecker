// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A program whose k-induction step case is far more expensive to unroll than its
// BMC base case, even for k = 1. Reduced from
// product-lines/elevator_spec13_product21.cil.c while investigating #1694.
//
// The base case starts at the program entry, where every global is 0, so all loops
// are skipped and exactly one path is feasible; it unrolls in ~0.1 s. The step case
// starts from an *arbitrary* state at each loop head, so every global is unknown,
// every loop can run 0, 1 or 2 times and both call sites of every Lk are reachable.
//
// The states that then pile up differ only in
//   (a) the per-loop iteration vector that LoopBoundCPA keeps in every state (ten
//       loops here: three per level plus the one in main), and
//   (b) which *invocation* of Lk they belong to -- CallstackState is compared by
//       object identity by design.
// Both are part of the reached-set partition key, so every state ends up in its own
// partition. In BMC mode merge^JOIN is the only mechanism that can collapse states
// at all (PredicateAbstractDomain covers a state only if the path formulas are
// identical), and merge is only ever attempted within a partition -- so nothing
// collapses and the step case enumerates paths instead of (location, state) pairs.
// The loop bound is respected throughout and the path formulas stay tiny (length
// <= 90); it is purely the number of states that explodes.
//
// This variant still terminates, so it is usable as a regression test. Adding more
// loops per level makes the step case stop terminating altogether, which is the
// behaviour reported in #1694.

extern void __VERIFIER_error() __attribute__((__noreturn__));

int c0 = 0;
int n0_0 = 0;
int n0_1 = 0;
int n0_2 = 0;
int c1 = 0;
int n1_0 = 0;
int n1_1 = 0;
int n1_2 = 0;
int c2 = 0;
int n2_0 = 0;
int n2_1 = 0;
int n2_2 = 0;
int acc = 0;

int L0(int x);
int L1(int x);
int L2(int x);
int L3(int x);

int L0(int x) {
  int j;

  j = 0;
  while (j < n0_0) {
    x = x + 1;
    j = j + 1;
  }
  j = 0;
  while (j < n0_1) {
    x = x + 1;
    j = j + 1;
  }
  j = 0;
  while (j < n0_2) {
    x = x + 1;
    j = j + 1;
  }

  if (c0 == 0) {
    return L1(x);
  }
  return L1(x + 1);
}

int L1(int x) {
  int j;

  j = 0;
  while (j < n1_0) {
    x = x + 1;
    j = j + 1;
  }
  j = 0;
  while (j < n1_1) {
    x = x + 1;
    j = j + 1;
  }
  j = 0;
  while (j < n1_2) {
    x = x + 1;
    j = j + 1;
  }

  if (c1 == 0) {
    return L2(x);
  }
  return L2(x + 1);
}

int L2(int x) {
  int j;

  j = 0;
  while (j < n2_0) {
    x = x + 1;
    j = j + 1;
  }
  j = 0;
  while (j < n2_1) {
    x = x + 1;
    j = j + 1;
  }
  j = 0;
  while (j < n2_2) {
    x = x + 1;
    j = j + 1;
  }

  if (c2 == 0) {
    return L3(x);
  }
  return L3(x + 1);
}

int L3(int x) {
  return x + 1;
}

int main(void) {
  int i = 0;

  while (i < 2) {
    acc = L0(acc);
    if (acc > 100) {
      acc = 0;
    }
    i = i + 1;
  }

  if (acc > 100000) {
    __VERIFIER_error();
  }
  return 0;
}
