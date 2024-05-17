// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(){
	const char s1[] = "\U000030b2";		//smg : false
	char s2[] = "\U000030b2";		//smg : true
	const char s3[] = "\U0000";		//smg : true
	return 0;
}
