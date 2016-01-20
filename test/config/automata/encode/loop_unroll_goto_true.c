extern int ldv_read_lock(void* p);
extern int ldv_read_unlock(void* p);
extern int ldv_write_lock(void* p);
extern int ldv_write_unlock(void* p);

int main(void) {
  int i = 0;
  int j = 0;

LOOPA:
  j = 0;

LOOPB:
  if (i < j) {
    void *l1 = (void *)0;
    ldv_read_lock(l1);
    ldv_read_unlock(l1);
  } else {
    void *l2 = (void *)0;
    ldv_read_lock(l2);
    ldv_write_lock(l2);
    ldv_write_unlock(l2);
    ldv_read_unlock(l2);
  }

  ++j;
  if (j < 13)
    goto LOOPB;

  ++i;
  if (i < 7)
    goto LOOPA;

  return 0;
}
