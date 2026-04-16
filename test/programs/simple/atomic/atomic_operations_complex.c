#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

_Atomic int x = 1;
int y = 1;
int z = 1;
int w = 1;

int f(int a) {
  return a + 1;
}

void *thread1(void *arg) {
  w = f(x++) + y - z--;
  return NULL;
}

void *thread2(void *arg) {
  z = 2;
}

int main() {
  pthread_t t1, t2;

  pthread_create(&t1, NULL, thread1, NULL);
  pthread_create(&t2, NULL, thread2, NULL);
  pthread_join(t1, NULL);
  pthread_join(t2, NULL);

  if (w == 2 && z == 1) {
    // this point is only reachable if the decrement is not interpreted as atomic
    ERROR: {reach_error();abort();}
  }

  return 0;
}
