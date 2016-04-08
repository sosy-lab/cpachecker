extern __VERIFIER_nondet_char();

int main() {
  signed char a = __VERIFIER_nondet_char();

  if (a < 0) {
    a++;
    if (a < 0) {
      a = -a;
    }
  }

  unsigned char b = a;

  a = a | 128;
  b = b | 128; 

  if (a == b) {
    goto ERROR;
  }
 
  a = a >> 1; // result: 11... or 01..., dependent on implementation of compiler
  b = b >> 1; // result: 01...

  if (a == b) {
ERROR:
    return -1;
  }
  
  return 0;
}
