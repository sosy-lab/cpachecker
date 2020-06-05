// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main(){
  
  int a;
  int b;
  int c;
 
  a = __VERIFIER_nondet_int();
  b = __VERIFIER_nondet_int();
  c = __VERIFIER_nondet_int();
 
  if(a == 1){
    a++;
    if(b == 1){
      b++;
      goto end;
    }
    else{
      b--;
      goto end;
    }
  }
  else{
    a--;
    if(c == 1){
      c++;
      goto end;
    }
    else{
      c--;
      goto end;
    }
  }

  end:
  ERROR:
  return(0);

}
