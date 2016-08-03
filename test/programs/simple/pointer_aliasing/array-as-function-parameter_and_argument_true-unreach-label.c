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

int med_fun(int v, int b[], int N) {
  if (v < 0) {
    return test_fun(b, N);
  } else {
    return v;
  }
}

int main() {
  int array_size = __VERIFIER_nondet_int();
  if (array_size < 1) {
     array_size = 1;
  }
  int* numbers = (int*) __builtin_alloca (array_size * sizeof(int));
  numbers[0] = 1;
  int res = med_fun(array_size - 100, numbers, array_size);
  if (res < 0) {
      ERROR: goto ERROR;
  }
  return 0;
}
