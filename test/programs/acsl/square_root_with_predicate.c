// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

//@ predicate is_positive(integer i) = i >= 0;

/*@ requires n >= 0;
    ensures \result >= 0;
    ensures \result * \result <= n;
    ensures n < (\result + 1) * (\result + 1);
*/
float sqroot(int n){
  
  int lowerBound = 0;
  int upperBound = n;
  float root = -1;

  while(lowerBound <= upperBound){
    int midPoint = 0.5 * (lowerBound + upperBound);
    
    if(midPoint * midPoint == n){
      return midPoint;
    }

    if(midPoint * midPoint < n){
      root = lowerBound;
      lowerBound = midPoint + 1;
    }
    if(midPoint * midPoint > n){
      upperBound = midPoint - 1;
    }
  }

  float increment = 0.1;
  for(int i = 0; i < 5; i++){
    while(root * root < n){
      root += increment;
    }
    root = root - increment;
    increment = increment / 10;
  }

  return root;
}

int main(){
  int n = __VERIFIER_nondet_int();
  float root = sqroot(n);
  //@ assert is_positive(root);

  if (root * root > n) {
    ERROR1: return -1;
  }
  if (n < (root+1) * (root+1)) {
    ERROR2: return -1;
  }

  return 0;
  
}
