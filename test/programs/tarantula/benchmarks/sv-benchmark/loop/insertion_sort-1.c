//SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//SPDX-License-Identifier: Apache-2.0
extern void abort(void); 
void reach_error(){}

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}
unsigned int __VERIFIER_nondet_uint();
int main() {
   unsigned int SIZE=3;
   int i, j, k, key;
   int v[SIZE];   
   for (j=1;j<SIZE;j++) {	  
      key = v[j];
      i = j - 1;
      while((i>=0) && (v[i]>key)) {
         if (i<2)
         v[i+100] = v[i];// error in array assignment v[i+100] instead of v[i+1]
         i = i - 1;
      }
      v[i+1] = key;	        
  }      
  for (k=1;k<SIZE;k++)
    __VERIFIER_assert(v[k-1]<=v[k]);  
   return 0;
}
