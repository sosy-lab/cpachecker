extern void __VERIFIER_error() __attribute__ ((__noreturn__));
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
extern _Bool __VERIFIER_nondet_bool();
extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int condition);

int main() {
    int i = __VERIFIER_nondet_int();
    if (i == i) {
        __VERIFIER_assert(i == i);
    }
    return 0;
}
