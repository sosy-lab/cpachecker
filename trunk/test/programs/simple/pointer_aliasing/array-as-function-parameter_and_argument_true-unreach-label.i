int __VERIFIER_nondet_int(void);

void *malloc(unsigned long size);

int global_array[2];

struct s { int value; int struct_array[2]; };

int target_function(int a[100], int N) {
    int i;
    int res = 0;
    if (a[0] != 0) {
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

int mediator_function(struct s s, int pointer_wanted[100], int array_wanted[100], int N) {
  if (global_array[0] != 1 || s.struct_array[0] != 2 || array_wanted[0] != 2) {
    ERROR: goto ERROR;
  }
  if (s.value >= 0) {
    return target_function(pointer_wanted, N);
  } else {
    return -s.value;
  }
}

int main() {
  int array_size = __VERIFIER_nondet_int();
  if (array_size < 1) {
     array_size = 1;
  }
  if (array_size > 3) {
    while (1);
  }
  int* numbers = (int*) malloc (array_size * sizeof(int));
  if (!numbers) { while(1); }
  numbers[0] = 0;
  global_array[0] = 1;
  struct s s;
  s.value = array_size - 100;
  int local_array[] = {2};
  s.struct_array[0] = local_array[0];
  struct s s_copy = s;
  int res = mediator_function(s_copy, numbers, local_array, array_size);
  if (res < 0) {
      ERROR: goto ERROR;
  }
  return 0;
}
