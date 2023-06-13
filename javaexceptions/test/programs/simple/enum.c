// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

enum e {
	E1,
	E2 = E1 + 3,
	E3
};

typedef enum {
        ENUM_1,
        enum1_force = 0x1122334455667788
} LLEnum;

typedef enum {
        ENUM_2,
        enum2_force = 0x112233
} SIEnum;

LLEnum enum1 = enum1_force;
SIEnum enum2 = enum2_force;

int main() {
	if (E1 != 0) {
		goto ERROR;
	}
	if (E2 != 3) {
		goto ERROR;
	}
	if (E3  != 4) {
		goto ERROR;
	}

    if (enum1 != enum1_force) { goto ERROR; }
    if (enum2 != enum2_force) { goto ERROR; }

	return 0;
ERROR:
	return 1;
}
