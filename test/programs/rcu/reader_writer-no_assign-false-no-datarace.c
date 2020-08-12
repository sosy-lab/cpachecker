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

void *writer(void * arg) {
  char * pWriter = calloc(3,sizeof(int));
  char * ptr = gp;
                      
  pWriter[0] = 'r';
  pWriter[1] = 'c';
  pWriter[2] = 'u';

  gp=pWriter; //BUG! No rcu_assign_pointer.

  ldv_synchronize_rcu();
  ldv_free(ptr);

  return 0;
}

int main(void) {

  gp = calloc(3,sizeof(int));

  pthread_t rd, wr;
  pthread_create(&rd, 0, reader, 0);
  pthread_create(&wr, 0, writer, 0);

  return 0;
}
