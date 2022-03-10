//SPDX-FileCopyrightText: Schindar Ali
//SPDX-License-Identifier: Apache-2.0
extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
int findMax(int arr[], int n) 
{ 
    int i; 
    int max = arr[0]; 
  
    
    for (i = 1; i < n; i++) {

        if (arr[i] > max) {
            max = arr[i]; 

          }
  }
    return max; 
} 
int findWorngMax(int arr[], int n) 
{ 
    int i; 
    int max = arr[1000]; // bug this should be int max = arr[0];
  
    
    for (i = n; i < n; i++) {

        if (arr[i] > max) {
            max = arr[i]; 

          }
  }
    return max; 
} 

int main() 
{ 
    int arr[] = {__VERIFIER_nondet_int(), __VERIFIER_nondet_int(), __VERIFIER_nondet_int(), __VERIFIER_nondet_int(), __VERIFIER_nondet_int()}; 
    int size = sizeof(arr)/sizeof(arr[0]);
    int correctMax = findMax(arr, size);
    int wrongMax = findWorngMax(arr, size);
    
    
    __VERIFIER_assert(correctMax == wrongMax);
    return 0; 
} 


