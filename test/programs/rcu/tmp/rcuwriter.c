void ldv_wlock_rcu(void) {

}

void ldv_wunlock_rcu(void) {

}

void ldv_free(void *) {

}

void ldv_synchronize_rcu(void) {

}

void ldv_rcu_assign_pointer(void * p1, void * p2) {

}

int main() {
  char * p = calloc(3 * sizeof(int));
  char * gp;
  char * ptr;
                      
  ptr = gp;
  p[1] = 'd';

  do {
    ldv_wlock_rcu();
    ldv_rcu_assign_pointer(gp, p);
    ldv_wunlock_rcu();
  } while(0);
  ldv_synchronize_rcu();
  ldv_free(ptr);

  return 0;
}
