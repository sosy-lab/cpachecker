// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Author: heizmann@informatik.uni-freiburg.de
// Date: 2015-09-09
//
// We assume sizeof(int)=4.

#include <stdio.h>

int main() {
	int x = 2147483647;
	x++;
	printf("%d\n", x);
	return 0;
}
