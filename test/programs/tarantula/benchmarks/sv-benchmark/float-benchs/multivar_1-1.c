extern void abort(void); 
void reach_error(){}
extern unsigned int __VERIFIER_nondet_uint(void);

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

int main(void) {
  unsigned int x = __VERIFIER_nondet_uint();
  unsigned int y = x;

  while (x < 1024) {
    x++;
    y= y+2;
  }

  __VERIFIER_assert(x == y);
}
