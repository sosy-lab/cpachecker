// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void f() { };

void main() {
	int status;
	int tmp;
	tmp = nondet_int();
	int x;
	if (tmp) {
		status = -1073741823L;
		x = 0;
	} else {
		status = 0;
	}

	f();

	if (status < 0L) {
ERROR:
		goto ERROR;
	} else {
	}
}
