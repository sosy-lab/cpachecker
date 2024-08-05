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
    // upper bound of  lld
    E3 = 9223372036854775807,
    // excceeding upper bound of lld
    E4,
    // lower bound of lld
    E5 = -9223372036854775808,
    // upper bound of llu
    E6 = 18446744073709551615,
    // calculating with upper bound of llu and upper bound of lld
    E7 = E6 - E3,
    // moving within space of llu
    E8 = E6 - 1
};

int main() {

	if (E1 != 0) {
		goto ERROR;
	}
	if (E2 != 1) {
		goto ERROR;
    }
    if (E3 != 9223372036854775807) {
        goto ERROR;
    }
    if (E4 != 9223372036854775808) {
        goto ERROR;
    }
    if (E5 != -9223372036854775808) {
        goto ERROR;
    }
    if (E6 != 18446744073709551615){
        goto ERROR;
    }
    if (E7 != 9223372036854775808){
        goto ERROR;
    }
    if (E8 != 18446744073709551614){
        goto ERROR;
    }

	return 0;
    
ERROR:
	return 1;
    
}
