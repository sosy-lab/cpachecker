// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int error() {
	// this function is actually never called
	int p;
	if (p) {
ERROR:
		goto ERROR;
	}
}

int main() {
	const int t = 1;
	const int f = 0;
	int rand;
	int tmp;

	t || error();
	f && error();

	if (t || error()) { }
	if (f && error()) { }

	tmp = t || error();
	tmp = f && error();

	tmp = (t || error()) ? (t || error()) : 0;
	tmp = (f && error()) ? 1 : (f && error());

	if (t > (f && error())) { }
	if (f < (t || error())) { }
	
	tmp = t > (f && error());
	tmp = f < (t || error());

	if (!!!(t || error())) { }
	if (!!!(f && error())) { }

	int test_multiple_operators_not_nested;
	(t || error()) + (f && error()) + tmp++;

	if (rand > 100 && tmp > 0 || rand <= 100 && tmp <= 1 || rand > 100 && tmp <= 1 || rand <= 100 && tmp > 0) { }
	else error();

	int test_expression_list_statement;
	return ({
		int __t = t;
		int __f = f;
		(f && error()) ? 1 : (f && error());
	} );
}
