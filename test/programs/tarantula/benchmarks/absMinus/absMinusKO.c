extern int __VERIFIER_nondet_uint();
extern void __VERIFIER_error();


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}

int absMinus (int i, int j) {
    int result;
    int k = 0;
    if (i <= j) {
        k = k+1;
    }
    if (k == 1 && i != j) {
        result = j-i; // error in the assignment : result = i-j instead of result = j-i
    }
    else {
        result = i-j;
    }
    __VERIFIER_assert( (i<j && result==j-i) || (i>=j && result==i-j));
}
int main() 
{ 
  
  absMinus(__VERIFIER_nondet_int(),__VERIFIER_nondet_int());
    return 0; 
} 