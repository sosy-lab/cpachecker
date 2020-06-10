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
#define a (2)
extern unsigned int __VERIFIER_nondet_uint();
int main() { 
  int i, j=10, n=4, sn=0;
  for(i=1; i<=n; i++) {
    if (i<j) 
    sn = n * a;// error this assignement should be removed
    j--;
  }
  __VERIFIER_assert(sn == 0);
}
