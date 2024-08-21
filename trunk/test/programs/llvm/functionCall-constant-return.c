// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error();

int f() {
  return 0;
}

int main() {
	int y;
	y = f();
	if (y != 0) {
ERROR:
    __VERIFIER_error();
    return 1;
	}
	return (y);
}
