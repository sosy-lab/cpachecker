// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2011-2013 Alexander von Rhein, University of Passau
// SPDX-FileCopyrightText: 2011-2021 The SV-Benchmarks Community
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// This is a heavily modified and simplified version of
// product-lines/minepump_spec4_product43.cil.c in SV-Benchmarks.

void reach_error() { __assert_fail("0","",0,""); }

extern int __VERIFIER_nondet_int(void);

// state
int w = 1, m = 0, p = 0, s = 1;

int main(void) {
  int i = 0, t1, t2, t3, t4;

  while (1) {
    if (i >= 1) {
        return 0;
    }

    if (s) {
      if (!p && w == 1 && !m)
        p = 1;
    }

    if (w == 0 && p)
      reach_error();
    i++;
  }

  return 0;
}

