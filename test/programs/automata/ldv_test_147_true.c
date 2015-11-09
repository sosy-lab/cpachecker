/* Test file for automaton cpa. Use with automaton specifications
 * ldv_147_assert.spc, ldv_147.spc */
void rcu_inc() {}
void rcu_dec() {}
void check_for_read_section() {}


void main()
{
  rcu_inc();
  rcu_dec();

  check_for_read_section();
}
