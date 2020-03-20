extern char __VERIFIER_nondet_char();
extern void __VERIFIER_error();

int main() {
  char a = __VERIFIER_nondet_char();
  char b = __VERIFIER_nondet_char();
  char c = __VERIFIER_nondet_char();

  if (a == 'a' && b == 5 && c == 16) {
  	b = 100;
  	// passed
  }
  if(c == 5) {
    }
    if(a == 1000){

    }
    if(b == 6){
       ERROR: __VERIFIER_error();
    }
    // passed
}
