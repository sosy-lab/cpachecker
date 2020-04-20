int __VERIFIER_nondet_int(void);

void *__builtin_alloca(unsigned long size);

int test_fun(int a[], int N) {
    int i;
    int res = 0;
    if (a[0] != 1) {
        ERROR: goto ERROR;
    }
    for (i = 0; i < N; i++) {
      while (a[i] > 0) {
          a[i]--;
          res++;
       }
    }
    return res;
}
int main() {
  int array_size = __VERIFIER_nondet_int();
  if (array_size < 1) {
     array_size = 1;
  }
  if (array_size > 3) {
    while (1);
  }
  int* numbers = (int*) __builtin_alloca (array_size * sizeof(int));
  numbers[0] = 1;
  int res = test_fun(numbers, array_size);
  if (res < 0) {
      ERROR: goto ERROR;
  }
  return 0;
}
