// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// this test case checks wether external functions with side-effects are handled correctly
int main() {
	int* p;
	int* q;
	p = malloc(4);
	q = malloc(4);
	if (p != q) {
ERROR:
		goto ERROR;
	}
}
