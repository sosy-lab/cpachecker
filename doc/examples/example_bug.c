extern void __VERIFIER_error();
extern char __VERIFIER_nondet_char();
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
} 

int main() {
  int i = 0;
  int a = 0;

  while (1) {
    if (i == 10) {
      goto LOOPEND;
    }

    if (i == 20) {
       goto LOOPEND;
    } else {
       i++;
       a++;
    }

  }

  LOOPEND:
  __VERIFIER_assert(i == 20);	
  return (1);


}

