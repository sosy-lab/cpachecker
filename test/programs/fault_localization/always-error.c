// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Lets FL produce an empty results set because there are no assertions.
int main() {
	int i = 0;
	i++;
	i--;
	goto ERROR;	
	
EXIT:
	return 0;
ERROR:
	return 1;
}
