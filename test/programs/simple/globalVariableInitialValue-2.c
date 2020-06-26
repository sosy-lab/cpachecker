// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Variables without storage class extern have 0 as default value.
int i;

int main() {
	if (i == 1) {
ERROR:		goto ERROR;
	}
}
