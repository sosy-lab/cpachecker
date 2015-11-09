/* Test file for automaton cpa. Use with automaton specification
 * ldv_32_1a_fixed.spc, ldv_32_1a.spc, ldv_32_1a_split.spc */
int mutex_trylock(void)
{
  int nondet;
  return nondet;
}

int null = 0;

void mutex_lock(void) {}
void mutex_unlock(void) {}

void ldv_check_final_state(void) {}

void main(void)
{
  if (mutex_trylock())
  {
    mutex_unlock();
  }

  ldv_check_final_state();
}
