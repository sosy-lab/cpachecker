// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main() {
	int x = 0;
	int y = 0;
	while (x < 10000) {
		x++;
		y++;
	}
  if (x != y) {
    ERROR: return 1;
  }
	return 0;
}
