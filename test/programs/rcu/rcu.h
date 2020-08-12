#include <pthread.h>
#include <stdlib.h>

void ldv_rcu_read_lock(void);

void ldv_rcu_read_unlock(void);

void ldv_rlock_rcu(void);

void ldv_runlock_rcu(void);

void * ldv_rcu_dereference(const void * pp);
void ldv_wlock_rcu(void);

void ldv_wunlock_rcu(void);

void ldv_free(void *);

void ldv_synchronize_rcu(void);

void ldv_rcu_assign_pointer(void * p1, const void * p2);
