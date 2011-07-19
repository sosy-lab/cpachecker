#include <pthread.h>
#include <assert.h>
#include <stdio.h>
// the program is unsafe: cs1=1 and cs2=1 can occur at the same time
int g = 0;
int cs1 = 0;
int cs2 = 0;

void *thr1() {
  cs1 = 1;
  g = 1;
  assert(cs2 == 0);
  cs1 = 0;
  pthread_exit(NULL);
}

void *thr2() {
  while (g != 1);
  g = 0;
  cs2 = 1;
  assert(cs1 == 0);
  pthread_exit(NULL);

}


void main() {
  pthread_t t1;
  pthread_t t2;
  long t;

  pthread_create(&t2, NULL, thr2, (void *)t);
  pthread_create(&t1, NULL, thr1, (void *)t);
  pthread_exit(NULL);
}

