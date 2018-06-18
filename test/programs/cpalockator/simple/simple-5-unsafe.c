/*
 * The ideal verdict differs from the ideal one: state is covered and usage is lost
 * The problem also is at visualization stage.
 */

typedef int pthread_mutex_t;
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int __VERIFIER_nondet_int();

int global;
pthread_mutex_t mutex;

int func(unsigned int cmd) {
  switch (cmd) {
    case (1UL | (unsigned long )3 ): 
      global++;
    break;
  }
  return 0;
}

int main() {

  func(1UL | (unsigned long )3);
}
