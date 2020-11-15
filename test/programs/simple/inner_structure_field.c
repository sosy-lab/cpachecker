// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct inner {
	int f;
};

struct outer {
	struct inner inner;
};

int main(void)
{
	struct outer outer, outer1;
	struct outer *pouter = &outer, *pouter1 = &outer1;
	pouter->inner.f = 0;
	pouter1->inner.f = 0;
	if (outer.inner.f != 0) {
		ERROR: return 1;
	} else {
		return 0;
	}
}
