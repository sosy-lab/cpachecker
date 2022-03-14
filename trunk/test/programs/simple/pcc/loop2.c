// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int a;
int j;

int main() {
  int i = 0;
  a = 0;
  j = 0;

  while(1) {
    if(i== 5){
       goto LOOPEND;
    }
    else{
       i++;
       a++;
    }
  }

  LOOPEND:

  if (a != 5) {
     goto ERROR;
  }
  else {}

while(1) {
    if(j== 5){
       goto LOOPEND2;
    }
    else{
       j++;
       a++;
    }
  }
LOOPEND2:
if(a!=10){
goto ERROR;
}
else{}
  
  return (0);
  ERROR:
  return (-1);
}

