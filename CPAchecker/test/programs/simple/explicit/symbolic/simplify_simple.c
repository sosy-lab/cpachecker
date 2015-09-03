extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();
  int b = a - a;

  if(b != 0) {
    goto ERROR;
  }
  
  return 0;

ERROR:
  return -1;
}


