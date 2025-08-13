  // This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern double nan(const char*);
extern double __VERIFIER_nondet_double();

int main() {
  double nan1 = nan("");
  double nan2 = 0.0/0.0;
  double nd = __VERIFIER_nondet_double();

  int c1 = (nd == 0.0);
  int c2 = (!(nd != 0.0));
  int c3 = (nd > 0.0);
  int c4 = (nd >= 0.0);
  int c5 = (nd < 0.0);
  int c6 = (nd <= 0.0);

  // Never true
  int c7 = (nd == nan1);
  int c8 = (nd == nan2);
  int c9 = (nd == nd);

  int n1 = !(nd == 0.0);
  int n2 = (nd != 0.0);
  int n3 = !(nd > 0.0);
  int n4 = !(nd >= 0.0);
  int n5 = !(nd < 0.0);
  int n6 = !(nd <= 0.0);
  int n7 = !(nd == nan1);
  int n8 = !(nd == nan2);
  int n9 = !(nd == nd);

  // Since we have a nondet these expressions all may evaluate to true
  if (c1) return 0;
  if (c2) return 0;
  if (c3) return 0;
  if (c4) return 0;
  if (c5) return 0;
  if (c6) return 0;
  if (c7) return 0;
  if (c8) return 0;
  if (c9) return 0;

  if (n1 && n2 && n3 && n4 && n5 && n6 && n7 && n8 && n9) {
    // Expected for nd being NaN
    ERROR:
      return 1;
  }

  return 0;
}