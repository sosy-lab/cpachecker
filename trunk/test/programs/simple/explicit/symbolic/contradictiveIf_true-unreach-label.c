extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();
  int b = __VERIFIER_nondet_int();
  int * c = &a;

  if (c != c) {
    goto ERROR;
  }

  if (a > a) {
    goto ERROR;
  }
  
  a = __VERIFIER_nondet_int();  

  if (a > b) {
    if (a < b) {
ERROR:
      return -1;
    }
  }

  return 0;
}
