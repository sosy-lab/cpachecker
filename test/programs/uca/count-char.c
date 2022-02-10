extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "sumt2.c", 3, "reach_error"); }
extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

char __VERIFIER_nondet_char();
int main() {
  int x=0;
  int y =0;
  char t = __VERIFIER_nondet_char();
 
  
  while(t != 'c'){
    x ++; y++;
    t = __VERIFIER_nondet_char();
  }
  while(x > 0){
    x--; y--;
  }
  __VERIFIER_assert(y == 0);
  return 0;
}

