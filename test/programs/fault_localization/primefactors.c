// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
void reach_error() { __assert_fail("0", "ss2f.c", 10, "reach_error"); }

int isPrime(int n){
    for(int i = 2; i <= n/2 + 1; i++){ // FIX: i < n/2 + 1 or i <= n/2
        if(n % i == 0) return 0;
    }
    return 1;
}

int main(){

    //Calculate prime factors of number;
    int number = 6;

    for(int i = 2; i <= number; i++){
        if (number % i == 0 && isPrime(i)) {
            number = number / i;
            i--;
        }
    }

    if(number != 1) {
        reach_error();
    }
    return 0;
}
