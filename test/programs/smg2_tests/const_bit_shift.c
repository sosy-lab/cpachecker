// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int arr[2];

void fun(int p){
	arr[p] = 2;		//smg false
}

int main(){
	int n = 2;
	fun( n >> 2 & 1  );

	return 0;	
}
