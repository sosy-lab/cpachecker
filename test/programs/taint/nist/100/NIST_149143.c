/* This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain. NIST assumes no responsibility whatsoever for its use by
 * other parties, and makes no guarantees, expressed or implied, about its
 * quality, reliability, or any other characteristic.

 * We would appreciate acknowledgement if the software is used.
 * The SAMATE project website is: http://samate.nist.gov
*/

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2010-2021 NIST
// SPDX-FileCopyrightText: 2021 The SV-Benchmarks Community
//
// SPDX-License-Identifier: CC0-1.0

#include <stdlib.h>
#include <string.h>

#define MAX_SIZE 10

static char __str[MAX_SIZE];

void cpyStr(const char *src) {
	strcpy(__str, src);								/* FLAW */
}

int main(int argc, char *argv[])
{
	// Often Misused String Management:
	// Buffer overflow with strcpy function
	if (argc > 1)
		cpyStr(argv[1]);	
	return 0;
}