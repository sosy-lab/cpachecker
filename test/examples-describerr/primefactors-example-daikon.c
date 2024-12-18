// This file is part of DescribErr,
// a tool for finding error conditions:
// https://gitlab.com/sosy-lab/software/describerr
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

void dummy(unsigned int x) {
}

int main(int argc, char *argv[]) {
	unsigned int lower_limit = 3;
	unsigned int x = atoi(argv[1]);
	unsigned int i = lower_limit;
	dummy(x,i);
	while (i <= x) {
		if (x % i == 0) {
		  x /= i;
		  i = lower_limit;
		} else i += 2;
	}
	return !(x <= 1);
}
