extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();

int id(int x) {
  if (x==0) return 0;
  return id(x-1) + 1;
}

void main() {
  int input = __VERIFIER_nondet_int();
  int result = id(input);
  if (result == 10) {
    __VERIFIER_error();
  }
}
