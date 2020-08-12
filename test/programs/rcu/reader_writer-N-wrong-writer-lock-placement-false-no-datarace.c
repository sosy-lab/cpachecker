#include "rcu.h"

char * gp;

void *reader(void * arg) {
    char *a;
    char b;
    char * pReader = &b;

    ldv_rcu_read_lock();
    a = ({typeof(gp) p;
    ldv_rlock_rcu();
    p = ldv_rcu_dereference(gp);
    ldv_runlock_rcu();
    p;});
    b = *a;
    ldv_rcu_read_unlock();
    
    return 0;
}

pthread_mutex_t mutex;

void *writer1(void * arg) {
  char * pWriter = calloc(3, sizeof(int));
  // BUG: mutex_lock should be here
  char * ptr = gp;
                      
  pWriter[0] = 'r';
  pWriter[1] = 'c';
  pWriter[2] = 'u';

  pthread_mutex_lock(&mutex);
  do {
    ldv_wlock_rcu();
    ldv_rcu_assign_pointer(gp, pWriter);
    ldv_wunlock_rcu();
  } while(0);
  pthread_mutex_unlock(&mutex);
  ldv_synchronize_rcu();
  ldv_free(ptr);

  return 0;
}

void *writer2(void * arg) {
  char * pWriter = calloc(3, sizeof(int));
  // BUG: mutex_lock should be here
  char * ptr = gp;
                      
  pWriter[0] = 'r';
  pWriter[1] = 'c';
  pWriter[2] = 'u';

  pthread_mutex_lock(&mutex);
  do {
    ldv_wlock_rcu();
    ldv_rcu_assign_pointer(gp, pWriter);
    ldv_wunlock_rcu();
  } while(0);
  pthread_mutex_unlock(&mutex);
  ldv_synchronize_rcu();
  ldv_free(ptr);

  return 0;
}

int main(void) {
  pthread_t rd, wr1, wr2;
  gp = calloc(3, sizeof(int));

  pthread_mutex_init(&mutex, ((void *)0));
  pthread_create(&rd, 0, reader, 0);
  pthread_create(&wr1, 0, writer1, 0);
  pthread_create(&wr2, 0, writer2, 0);

  return 0;
}
