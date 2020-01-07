extern void __VERIFIER_error(void);
extern void __VERIFIER_assume(int);
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}

int main() {
    for (int i=0; i<100; i++) {
        __VERIFIER_assert(i>=0);
        __VERIFIER_assert(i>=0);
    }
}
