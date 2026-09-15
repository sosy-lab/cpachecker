// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// testing upper and lower bounds of unsigned long long
enum e1 {
    // lower bound of llu
    E1,
    E2,
    // upper bound of lld
    E3 = 9223372036854775807L,
    // beyond upper bound of lld, enum must become a llu now
    E4,
    // lower bound of llu
    E5 = 0,
    // close to upper bound of llu
    E6 = 18446744073709551614UL,
    // upper bound of llu
    E7,
    // calculating with two llus
    E8 = E7 - E6,
    // moving within space of llu
    E9 = E6 - 18446744073709551614UL
};

int main() {

	if (E1 != 0) {
		goto ERROR;
	}
	if (E2 != 1) {
		goto ERROR;
    }
    if (E3 != 9223372036854775807UL) {
        goto ERROR;
    }
    if (E4 != 9223372036854775808UL) {
        goto ERROR;
    }
    if (E5 != 0) {
        goto ERROR;
    }
    if (E6 != 18446744073709551614UL){
        goto ERROR;
    }
    if (E7 != 18446744073709551615UL){
        goto ERROR;
    }
    if (E8 != 1){
        goto ERROR;
    }
    if (E9 != 0){
        goto ERROR;
    }

	return 0;
    
ERROR:
	return 1;
    
}
