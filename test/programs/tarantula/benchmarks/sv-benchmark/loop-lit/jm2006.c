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
    int i, j;
    i = __VERIFIER_nondet_int();
    j = __VERIFIER_nondet_int();
    if (!(i >= 0 && j >= 0)) return 0;
    int x = i;
    int y = j;
    while(x != 0) {
        x--;
        y = y-3; // error this should be y--
    }

    if (i == j) {
        __VERIFIER_assert(y == 0);
    }
    return 0;
}
