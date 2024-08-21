// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function);

extern int __VERIFIER_nondet_int(void);

// Checks whether there are 5 or more non-prime numbers between 2 and 100.
int main() {
  int nonprimesCount = 0;
  int lastN = 2;
  while (1) {
    // Choose a number n, greater than the last number and <= 100.
    int choiceN = __VERIFIER_nondet_int();
    if (choiceN <= lastN || choiceN > 100) {
      break;
    }
    lastN = choiceN;
    // Choose a number div, >= 2 and smaller than the choiceN.
    int choiceDiv = __VERIFIER_nondet_int();
    if (choiceDiv < 2 || choiceDiv >= choiceN) {
      break;
    }
    // If 'n' is divisible by 'div', it is not a prime.
    // Then increase the non-prime counter by one.
    if (choiceN % choiceDiv == 0) {
      nonprimesCount++;
    }
  }

  // If we found at least 5 non-primes, fail with an assertion.
  if (nonprimesCount >= 5) {
    __assert_fail("There are 5 or more non-prime numbers between 2 and 100", "example_bug.c", 39, "main");
  }
}

