
void __VERIFIER_assert(int cond) {
  if (!cond) {
    ERROR:
    goto ERROR;
  }
}

int main() {
  int i = 0;
  int x = 0;
  i++;
  while (x<10) {
    if (i>0) {
      i--;
    } else {
      i++;
    }
    __VERIFIER_assert(x<5);
    __VERIFIER_assert(i<2);
    x++;
  }
}
