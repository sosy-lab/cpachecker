#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

int counter = 0;
int x = 0;
int y = 0;

void *thread1(void *arg) {
  x = ++counter;
  return NULL;
}

void *thread2(void *arg) {
  y = ++counter;
  return NULL;
}

int main() {
  pthread_t t1, t2;

  pthread_create(&t1, NULL, thread1, NULL);
  pthread_create(&t2, NULL, thread2, NULL);
  pthread_join(t1, NULL);
  pthread_join(t2, NULL);
  if (x == y) {
    // this point is only reachable if the increment is not interpreted as atomic
    ERROR: {reach_error();abort();}
  }

  return 0;
}
