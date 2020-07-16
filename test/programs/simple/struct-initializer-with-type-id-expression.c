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
	void *p;
};

struct t t = { (void *) & (struct s) { .x = 42 } };

int main() {
	if (((struct s *)(t.p))->x != 42) {
ERROR:
		return 1;
	}
	return 0;
}
