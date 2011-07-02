#include <pthread.h>
#include <assert.h>
#include <stdio.h>
// the program is safe: cs1=1 and cs2=1 never occur at the same time
int g = 0;
int cs1 = 0;
int cs2 = 0;

void *thr1() {
  printf("thread one created\n");
  cs1 = 1;
  g = 1;
  assert(cs2 == 0);
  cs1 = 0;
  printf("thread one exits\n");
  pthread_exit(NULL);
}

void *thr2() {
  printf("thread two created\n");
  while (g != 1);
  g = 0;
  cs2 = 1;
  assert(cs1 == 0);
  printf("thread two exits\n");
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

