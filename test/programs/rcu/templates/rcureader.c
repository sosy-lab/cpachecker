void ldv_rcu_read_lock(void) {

}

void ldv_rcu_read_unlock(void) {

}

void ldv_rlock_rcu(void) {

}

void ldv_runlock_rcu(void) {

}

void * ldv_rcu_dereference(void * pp) {

}

int main() {
    char *a;
    char * gp;
    char b;
    char * p = &b;

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
