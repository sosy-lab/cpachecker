#include "rcu.h"

struct foo {
  void * gp;
} * pStruct;

struct bar {
  char * ptr;
};

void *reader(void * arg) {
    char *a;
    char b;
    char * pReader = &b;
    struct bar * p1 = calloc(1, sizeof(struct bar));

    ldv_rcu_read_lock();
    p1 -> ptr = ({typeof(pStruct -> gp) p;
      ldv_rlock_rcu();
      p = ldv_rcu_dereference(pStruct -> gp);
      ldv_runlock_rcu();
      p;});
    a = p1 -> ptr;
    b = *a;
    ldv_rcu_read_unlock();

    return 0;
}

pthread_mutex_t mutex;

void *writer(void * arg) {
  char * pWriter = calloc(3, sizeof(int));
  struct bar * p = calloc(1, sizeof(struct bar));
  p -> ptr = pStruct -> gp;

  pWriter[0] = 'r';
  pWriter[1] = 'c';
  pWriter[2] = 'u';

  do {
    ldv_wlock_rcu();
    ldv_rcu_assign_pointer(pStruct -> gp, pWriter);
    ldv_wunlock_rcu();
  } while(0);
  //ldv_synchronize_rcu(); //BUG is here
  ldv_free(p -> ptr);

  return 0;
}

int main(void) {
  pthread_t rd, wr;
  pStruct = calloc(1, sizeof(struct foo));
  pStruct -> gp = calloc(3, sizeof(int));

  pthread_mutex_init(&mutex, ((void *)0));
  pthread_create(&rd, 0, reader, 0);
  pthread_create(&wr, 0, writer, 0);

  return 0;
}
