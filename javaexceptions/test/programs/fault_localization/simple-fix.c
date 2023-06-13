// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(){
	
	int a = 1;
	int b = 2;
	if(a < b)
		if(a < b)
			goto ERROR;


EXIT: return 0;
ERROR: return 1;
}
