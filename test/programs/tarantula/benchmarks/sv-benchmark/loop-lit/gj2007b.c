//SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//SPDX-License-Identifier: Apache-2.0
extern __VERIFIER_nondet_int();
extern void __VERIFIER_error();

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}


int main() {
    int x = 0;
    int m = 0;
    int n = __VERIFIER_nondet_int();
    while(x < n) {
	if(__VERIFIER_nondet_int()) {
	    m = x;
      m = m+10;//error: unnecessary assignment, should be removed.
	}
	x = x + 1;
    }
    __VERIFIER_assert((m >= 0 || n <= 0));
    __VERIFIER_assert((m < n || n <= 0));
    return 0;
}
