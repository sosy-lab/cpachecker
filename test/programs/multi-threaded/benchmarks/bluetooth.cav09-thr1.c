#include <assert.h>


int stopped = 0, driverStoppingFlag = 0, stoppingEvent = 0; // boolean flags
int pendingIo = 1;

inline void IoDec() {
  int PIo = pendingIo-1;
  pendingIo = pendingIo-1;
  if (PIo == 0) { stoppingEvent = 1; }
}


void main() {
  driverStoppingFlag = 1;
  IoDec();
  while (stoppingEvent <= 0);
  stopped = 1;
}

/*
int main() {
  pthread_t t1, t2, t3, t4;
  pthread_create(&t1, NULL, adder, NULL);
  pthread_create(&t2, NULL, stopper, NULL);
  pthread_create(&t3, NULL, adder, NULL);
  pthread_create(&t4, NULL, stopper, NULL);
}
*/
