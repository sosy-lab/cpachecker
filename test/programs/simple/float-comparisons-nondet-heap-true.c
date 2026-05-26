// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *malloc(unsigned long);
extern void free(void*);
extern double nan(const char*);
extern double __VERIFIER_nondet_double();

struct MyDouble {
  double value;
};

// Test program (and export through CEX and witnesses) for NaN values in memory structures 
// (can behave differently in export compared to normal variables in CPAchecker)
int main() {
  struct MyDouble nan1;
  nan1.value = nan("");

  struct MyDouble * nan3 = malloc(sizeof(struct MyDouble));
  if (nan3 == 0) {
    return 0;
  }
  nan3->value = nan("");

  double * nan2 = malloc(sizeof(double));
  if (nan2 == 0) {
    return 0;
  }
  *nan2 = 0.0/0.0;

  struct MyDouble * nd1 = malloc(sizeof(struct MyDouble));
  if (nd1 == 0) {
    return 0;
  }
  nd1->value = __VERIFIER_nondet_double();

  double * nd2 = malloc(sizeof(double));
  if (nd2 == 0) {
    return 0;
  }
  *nd2 = __VERIFIER_nondet_double();

  int c1 = (nd1->value == 0.0);
  int c2 = (!(nd1->value != 0.0));
  int c3 = (nd1->value > 0.0);
  int c4 = (nd1->value >= 0.0);
  int c5 = (nd1->value < 0.0);
  int c6 = (nd1->value <= 0.0);

  // Never true
  int c7 = (nd1->value == nan1.value);
  int c8 = (nd1->value == (*nan2));
  int c9 = (nd1->value == nd1->value);
  int c0 = (nd1->value == nan3->value);

  int n1 = !(nd1->value == 0.0);
  int n2 = (nd1->value != 0.0);
  int n3 = !(nd1->value > 0.0);
  int n4 = !(nd1->value >= 0.0);
  int n5 = !(nd1->value < 0.0);
  int n6 = !(nd1->value <= 0.0);
  int n7 = !(nd1->value == nan1.value);
  int n8 = !(nd1->value == (*nan2));
  int n9 = !(nd1->value == nd1->value);
  int n0 = !(nd1->value == nan3->value);

  int c12 = (*nd2 == 0.0);
  int c22 = (!(*nd2 != 0.0));
  int c32 = (*nd2 > 0.0);
  int c42 = (*nd2 >= 0.0);
  int c52 = (*nd2 < 0.0);
  int c62 = (*nd2 <= 0.0);

  // Never true
  int c72 = (*nd2 == nan1.value);
  int c82 = (*nd2 == (*nan2));
  int c92 = (*nd2 == *nd2);
  int c02 = (*nd2 == nan3->value);

  int n12 = !(*nd2 == 0.0);
  int n22 = (*nd2 != 0.0);
  int n32 = !(*nd2 > 0.0);
  int n42 = !(*nd2 >= 0.0);
  int n52 = !(*nd2 < 0.0);
  int n62 = !(*nd2 <= 0.0);
  int n72 = !(*nd2 == nan1.value);
  int n82 = !(*nd2 == (*nan2));
  int n92 = !(*nd2 == *nd2);
  int n02 = !(*nd2 == nan3->value);

  if (c7) goto ERROR;
  if (c8) goto ERROR;
  if (c0) goto ERROR;

  if (c72) goto ERROR;
  if (c82) goto ERROR;
  if (c02) goto ERROR;

  // Since we have a nondet these expressions all may evaluate to true
  if (c1) goto NOERROR;
  if (c2) goto NOERROR;
  if (c3) goto NOERROR;
  if (c4) goto NOERROR;
  if (c5) goto NOERROR;
  if (c6) goto NOERROR;

  // By this point nd can only be NaN
  if (c9) goto ERROR;

  if (c12) goto NOERROR;
  if (c22) goto NOERROR;
  if (c32) goto NOERROR;
  if (c42) goto NOERROR;
  if (c52) goto NOERROR;
  if (c62) goto NOERROR;

  // By this point nd can only be NaN
  if (c92) goto ERROR;

  if (n1 && n2 && n3 && n4 && n5 && n6 && n7 && n8 && n9 && n0 && n12 && n22 && n32 && n42 && n52 && n62 && n72 && n82 && n92 && n02) {
    // Expected for values in nd1 and nd2 being NaN
    NOERROR:
    free(nan3);
    free(nan2);
    free(nd1);
    free(nd2);
    return 0;
  }

  ERROR:
  free(nan3);
  free(nan2);
  free(nd1);
  free(nd2);
  return 1;
}
