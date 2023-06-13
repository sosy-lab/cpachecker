// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void){
  
  int a;
  int b;
  int c;
 
  a = 5;
  b = a + 1;
 
  c = asdad();
  b = sdsd();
 
  if(c == 89){
    b = 90;
  }
  else{
   b = 2000;
  }

  c = b;
  a = c - 1;
  
  if(a == 89){
  errorFn();
  }
  return (0);
 
}

void errorFn(void) 
{ 
  goto ERROR;
  ERROR: 
  return;
}
