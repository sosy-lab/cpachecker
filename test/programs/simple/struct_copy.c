// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// this test case checks wether struct copies are handled
struct s { int x; };
int main() {
	struct s a;
	struct s b;
	a.x = 4;
	b.x = 8;
	a = b;
	if (a.x != b.x) {
ERROR:
		goto ERROR;
	}
}
