extern int __VERIFIER_nondet_uint();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
int largest(int arr[], int num2);
int main() 
{ 
    int arr[] = {10, 324, 45, 90, 9808}; 
    int n = 5; 
    int target = largest(arr, n);
    __VERIFIER_assert(target == 9808);
    return 0; 
} 

int largest(int arr[], int n) 
{ 
    int i; 
    int max = arr[0]; 
  
    
    for (i = 1; i < n; i++) {

        if (arr[i] > max) {
            max = -arr[i]; 

          }
  }
    return max; 
} 
