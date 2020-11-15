// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Variables with storage class extern do not have any default value!
extern int i;

int main() {
	if (i == 1) {
ERROR:		goto ERROR;
	}
}
