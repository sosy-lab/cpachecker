extern int ldv_read_lock(void* p);
extern int ldv_read_unlock(void* p);
extern int ldv_write_lock(void* p);
extern int ldv_write_unlock(void* p);

int main(void) {
  int i;
  for (i = 0; i < 7; ++i) {
    int j;
    for (j = 0; j < 13; ++j) {
      if (i < j) {
        void* l1 = (void*) 0;
        ldv_read_lock(l1);
        ldv_read_unlock(l1);
      } else {
        void* l2 = (void*) 0;
        ldv_read_lock(l2);
        ldv_write_lock(l2);
        ldv_write_unlock(l2);
        ldv_read_unlock(l2);
      }
    }
  }

  return 0;
}
