// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>

#ifdef BLAST_AUTO_1
int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;
#else

int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;

//special assert
void check_error(int b) {
	assert(b);
}
#endif

int main(void) {
	int i=0;
        while(i<5) {
                i++;
#ifdef BLAST_AUTO_1
                assert(i!=3);
#else
		check_error(i!=3);
#endif
        }
	return 0;
}
