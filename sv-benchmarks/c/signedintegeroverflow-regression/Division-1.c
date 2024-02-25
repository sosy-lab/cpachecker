// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Author: heizmann@informatik.uni-freiburg.de
// Date: 2015-09-01
//
// We assume sizeof(int)=4.

#include <stdio.h>

int main() {
	int minInt = -2147483647 - 1;
	int x = (minInt / -1 ) - 1;
	printf("%d\n", x);
	return 0;
}
