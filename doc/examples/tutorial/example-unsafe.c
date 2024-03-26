extern unsigned __VERIFIER_nondet_uint();
int main() {
  unsigned n =  __VERIFIER_nondet_uint();
  unsigned x =  __VERIFIER_nondet_uint();
  unsigned y = n - x;
  while(x > y) {
    x--; y++;
    if (x < y) {
      ERROR: return 1;
    }
  }
  return 0;
}
