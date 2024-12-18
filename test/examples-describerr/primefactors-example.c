// This file is part of DescribErr,
// a tool for finding error conditions:
// https://gitlab.com/sosy-lab/software/describerr
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_char();

extern void abort(void);
void reach_error() {}

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

void assume(int cond) {
    if (!cond) abort();
}

int main() {
    char x = __VERIFIER_nondet_char(); // input
    // misses negative numbers and
    // >1 occurrences of factor 2
    if (x % 2 == 0) x /= 2;
    char i = 3;
    while (i <= x) {
        if (x % i == 0) {
            x = x / i;
            i = 3;
            continue;
        }
        i = i + 2;
    }
    __VERIFIER_assert(x == 1);
}