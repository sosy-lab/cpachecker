extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern unsigned int __VERIFIER_nondet_uint(void);
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
extern _Bool __VERIFIER_nondet_bool();

int main() {
  int i=0;
  while (__VERIFIER_nondet_bool()) {
    for (i=0; i<10; i++) {
    }
    i += 10;
  }
  __VERIFIER_assert(i >= 0 && i <= 20);
}
