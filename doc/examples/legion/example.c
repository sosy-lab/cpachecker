extern void __VERIFIER_error() __attribute__ ((__noreturn__));

int main() {
  int a = __VERIFIER_nondet_int();
  if (a == 0){
    int b = __VERIFIER_nondet_int();
    if (b < -1610612736){
      a = 2;
      int c = __VERIFIER_nondet_int();
      if (c < -1){
        a = 3;
        __VERIFIER_error();
      }
    }
  }
}
