extern int __VERIFIER_nondet_uint();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}

int absMinus (int i, int j) {
    int result=i+1; // error in the assignment : result = i instead of result = i+1
    int k = 0;
    if (i <= j) {
        k = k+1;
    }
    if (k == 1 && i != j) {
        result = j-result;    
    }
    else {
        result = result-j;
    }
    
    __VERIFIER_assert( (i<j && result==j-i) || (i>=j && result==i-j));
}
int main() 
{ 
  
  absMinus(__VERIFIER_nondet_int(),__VERIFIER_nondet_int());
    return 0; 
} 