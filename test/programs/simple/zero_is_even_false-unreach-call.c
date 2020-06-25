extern void __VERIFIER_error() __attribute__((__noreturn__));
void __VERIFIER_assert(int cond) {
    if (!cond) {
      ERROR: __VERIFIER_error();
    }
    return;
}
extern int __VERIFIER_nondet_int();
int main() {
  int n = __VERIFIER_nondet_int();
  __VERIFIER_assert((2*n) != 0);
}
