extern void __VERIFIER_error() __attribute__((__noreturn__));
void __VERIFIER_assert(int cond) {
    if (!cond) {
      ERROR: __VERIFIER_error();
    }
}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  __VERIFIER_assert((x == 1) != 99 && x == 1);
}
