// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char*, const char*, unsigned, const char*);
void reach_error() { __assert_fail("0","",0,""); }

extern int __VERIFIER_nondet_int(void);

/* state */
int w = 1, m = 0, p = 0, s = 1;

/* error */
void __automaton_fail(void) {
  ERROR: {reach_error(); abort();}
}

/* one step */
void timeShift(void) {
  if (p && w > 0) w--;              /* lowerWaterLevel */

  if (s) {                          /* systemActive */
    if (!p && w == 2 && !m)         /* processEnvironment + methane check */
      p = 1;
  }

  if (w == 0 && p)                  /* specification */
    __automaton_fail();
}

/* main test loop */
int main(void) {
  int i = 0, t1, t2, t3, t4;

  while (1) {
    while_0_continue: ;
    if (i < 4) {
    } else {
      goto while_0_break;
    }

    t1 = __VERIFIER_nondet_int();
    if (t1 && w < 2) w++;           /* waterRise */

    t2 = __VERIFIER_nondet_int();
    if (t2) m = !m;                 /* changeMethane */

    t3 = __VERIFIER_nondet_int();
    if (t3) {
    } else {
      t4 = __VERIFIER_nondet_int();
      if (t4) {                     /* stopSystem */
        if (p) p = 0;
        s = 0;
      }
    }

    timeShift();
    i++;
  }
  while_0_break: ;

  /* cleanup */
  i = 0;
  timeShift();

  while (1) {
    while_1_continue: ;
    if (i < 3) {
    } else {
      goto while_1_break;
    }
    timeShift();
    i++;
  }
  while_1_break: ;

  return 0;
}
