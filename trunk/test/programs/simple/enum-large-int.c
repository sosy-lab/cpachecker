// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// testing upper and lower bounds of int
enum e1 {
	E1,
    E2,
    // upper bound of type int
    E3 = 2147483647,
    // exceeding upper bound of type int
    E4,
    // moving within space of type int
    E5 = E3 - 1,
    // lower bound of type int
    E6 = -2147483648,
    // moving within space of type int
    E7,
    // calculation with upper and lower bound of type int
    E8 = E3 + E6,
};

// testing upper bound of unsigned int
enum e2 {
    // upper bound of u
    E9 = 4294967295,
    // moving within space of u
    E10 = E9 - 1
};

int main() {

	if (E1 != 0) {
		goto ERROR;
	}
	if (E2 != 1) {
		goto ERROR;
    }
    if (E3 != 2147483647) {
        goto ERROR;
    }
    if (E4 != 2147483648) {
        goto ERROR;
    }
    if (E5 != 2147483646) {
        goto ERROR;
    }
    if (E6 != -2147483648) {
        goto ERROR;
    }
    if (E7 != -2147483647) {
        goto ERROR;
    }
    if (E8 != -1) {
        goto ERROR;
    }
    if (E9 != 4294967295) {
        goto ERROR;
    }
    if (E10 != 4294967294) {
        goto ERROR;
    }

	return 0;
    
ERROR:
	return 1;
    
}
