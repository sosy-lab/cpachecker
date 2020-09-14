extern void abort(void); 
void reach_error(){}

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

int main() {
  int x=0;

  while(1)
  {
    __VERIFIER_assert(x==8);    // error in the assertion: x==8 instead of x==0
  }

 
}
