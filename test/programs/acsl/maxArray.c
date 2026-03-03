// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

/*@ logic integer max_array(int* array, integer length) =
      (length == 1) ? aray[1] : max_array(array, length -1);
*/

/*@
 assigns \nothing;
 ensures \result >= a && \result >= b;
 ensures \result == a || \result == b;
 */
int max(int a, int b) {
    return a>=b ? a:b;
}

/*@ requires l > 0;
    assigns \nothing;
    ensures \result = max_array(a, l);
 */
int maxArray (int *a, int l) {
  int m = a[0];

/*@ loop invariant 0 <= i <= l;
  loop invariant m == max_array(a,i);
 */
  for(int i = 0; i < l; i++) {
    m = max(m, a[i]);
  }
  return m;
}

int main(){
  int a[50];
  for(int i = 0; i < 50; i++){
    a[i] = __VERIFIER_nondet_int();
  }

  int m = maxArray(a, 50);
  //@ assert m == max_array(a, 50);

  int j = -1;
  while (j < 0 || j >= 50) {
    j = __VERIFIER_nondet_int();
  }
  if(m < a[j]){
    ERROR:
    return -1;
  }

  return 0;
}
