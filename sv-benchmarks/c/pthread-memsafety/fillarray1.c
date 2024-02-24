#include <pthread.h>
#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

int size;
int ind;
int j = 2, i = 1;
int x, y, z;

void *t1(void *arg) {
  x = 1;
  y = 1;
  int *a = (int *)arg;
  while (y == 1 && z) {
  }
  while (ind < size - 1) {
    ++ind;
    a[ind] = i;
  }
  x = 0;

  pthread_exit(NULL);
}

void *t2(void *arg) {
  z = 1;
  y = 0;
  int *a = (int *)arg;
  while (y == 0 && x) {
  }
  while (ind < size - 1) {
    ++ind;
    a[ind] = j;
  }
  z = 0;

  pthread_exit(NULL);
}

int main(int argc, char **argv) {

  size = __VERIFIER_nondet_int();

  if (size < 1 || size > 20) {
    return 0;
  }

  int *a = (int *)malloc(size);
  pthread_t id1, id2;

  ind = 0;

  pthread_create(&id1, NULL, t1, a);
  pthread_create(&id2, NULL, t2, a);

  free(a);
  return 0;
}
