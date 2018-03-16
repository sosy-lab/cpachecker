typedef int pthread_mutex_t;
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int __VERIFIER_nondet_int();

int global;
pthread_mutex_t mutex;

int func(unsigned int cmd) {
  switch (cmd) {
    case (1UL | (unsigned long )4 ): 
      global++;
    break;
  }
}

int main() {

  func(1UL | (unsigned long )3);
}
