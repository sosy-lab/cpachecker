// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
	int __BLAST_NONDET;
	int x;
	int y;
	x = __BLAST_NONDET;
	y = __BLAST_NONDET;
	if (x != y) {
ERROR:
		goto ERROR;
	}
}
