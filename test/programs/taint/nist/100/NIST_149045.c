/*PLOVER: BUFF.OVER, BUFF.FORMAT*/

/*
Description: Printf is called with a user supplied format string.
Keywords: Size0 Complex0 Taint FormatString
ValidArg: "'NormalString\n'"
InvalidArg: "%s"*100
*/
// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2010-2021 NIST
// SPDX-FileCopyrightText: 2021 The SV-Benchmarks Community
//
// SPDX-License-Identifier: CC0-1.0


#include <stdio.h>

void
test(char *str)
{
	printf(str);				/* FLAW */
}

int
main(int argc, char **argv)
{
	char *userstr;

	if(argc > 1) {
		userstr = argv[1];
		test(userstr);
	}
	return 0;
}
