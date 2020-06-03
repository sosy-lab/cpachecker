// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#ifndef REAL_HEADERS
  #include "cpalien-headers.h"
#else
  #include <stdio.h>
  #include <stdlib.h>
#endif

int do_an_allocation() {
	int *a = malloc(sizeof(int));
	int *b = a;
	*a = 4;
	free(b);
	return 48;
}

int main(int argc, char* argv[]){
	int i = do_an_allocation();
	return i % 2;
}
