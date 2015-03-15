extern __VERIFIER_nondet_int();

int main() {
 int a = __VERIFIER_nondet_int();
 int b = a;

 a = a + 10;
 b =  5 + b + 5;

 if (a != b) {
ERROR:
  return -1;
 }
}

