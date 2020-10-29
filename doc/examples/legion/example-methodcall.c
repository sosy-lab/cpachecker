extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int(void);

void check (int a){
  int b, c;
  if (a == 0){
    b = __VERIFIER_nondet_int();
    if (b < -1610612736){
      a = 2;
      c = __VERIFIER_nondet_int();
      if (c < -1){
        a = 3;
      }
    }
  }
}

int main() {
  int a;
  
  a = __VERIFIER_nondet_int();

  check(a);

}
