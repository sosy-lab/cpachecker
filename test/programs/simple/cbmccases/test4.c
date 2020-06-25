// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(){
  
  int a;
  int b;
  int c;
 
  a = __BLAST_NONDET;
  b = __BLAST_NONDET;
  c = __BLAST_NONDET;  
 
  if(a == 1){
    goto end;
  }

  else{
    if(b == 1){
      if(c ==1){
        c++;
        goto end;
      }
      else{
        
      }
    }
    else{
      goto lab;
    }
    b--;
    lab: ;
    a++;
    goto end;
  }

  end:

  ERROR:
  return (1);

}
