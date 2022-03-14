// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(){
int a=0;
int i=0;

for(;i<10;i++){
	a++;
}

if(a!=i ||  i!=10){
	goto ERROR;
}

return 0;

ERROR: 
return -1;}
