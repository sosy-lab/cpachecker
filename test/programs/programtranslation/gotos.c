unsigned char __VERIFIER_nondet_uchar(void);
void __VERIFIER_error();
void __VERIFIER_assume(int cond);

int main() {
  int x = __VERIFIER_nondet_uchar();
  if (x > 0) {
    goto return_label;
  } else {
    x = x + 1;
  }
  if (x > 0) {
    __VERIFIER_error();
  } else {
    goto return_label;
  }


  return_label:
  return 0;
}
