extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();

int id(int x) {
  if (x==0) return 0;
  return id2(x-1) + 1;
}

int id2(int x) {
  if (x==0) return 0;
  return id(x-1) + 1;
}

void main() {
  int input = 5;
  int result = id(input);
  if (result != 5) {
    __VERIFIER_error();
  }
}
