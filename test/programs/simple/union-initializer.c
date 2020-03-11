union u {
   int i1 ;
   int i2 ;
};

union u u[1] = {{ .i1 = 1 }};

extern void __VERIFIER_error() __attribute__ ((__noreturn__));
int main(void) {
  if (u[0].i1 == 1) {
ERROR:
    return 1;
  }
  return 0;
}
