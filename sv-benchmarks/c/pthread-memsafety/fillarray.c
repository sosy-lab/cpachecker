#include <pthread.h>
#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

int size;
int ind;
int j = 2, i = 1;

void *t1(void *arg) {
  int *a = (int *)arg;
  while (ind < size - 1) {
    ++ind;
    a[ind] = i;
  }

  pthread_exit(NULL);
}

void *t2(void *arg) {
  int *a = (int *)arg;
  while (ind < size - 1) {
    ++ind;
    a[ind] = j;
  }
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
