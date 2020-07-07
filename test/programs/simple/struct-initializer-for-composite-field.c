// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct s {
	int x;
};
struct t {
	struct s s;
};
int main() {
	const struct s s = { .x = 1 };
	struct t t = { .s = s };
ERROR:
	if (t.s.x == 1) {
		return 0;
	} else {
		return 1;
	}
}
