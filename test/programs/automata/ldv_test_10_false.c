/* Test file for automaton cpa. Use with automaton specifications
 * ldv_10_1a_A.spc, ldv_10_1a_B.spc */
int LDV_IN_INTERRUPT;

void alloc_atomic(int flags)
{
  if (LDV_IN_INTERRUPT == 2 && flags == 32)
    goto ERROR;
  return;
ERROR:
  return;
}

void main()
{
  LDV_IN_INTERRUPT = 1;
  alloc_atomic(32);
  alloc_atomic(2);
  int nondet;
  if (nondet)
  {
    LDV_IN_INTERRUPT = 2;
    alloc_atomic(2);
    alloc_atomic(0);
    LDV_IN_INTERRUPT = 1;
  }
  LDV_IN_INTERRUPT = 2;
  alloc_atomic(32);
  LDV_IN_INTERRUPT = 1;
  alloc_atomic(2);
  alloc_atomic(32);
}
