/* Test file for automaton cpa. Use with automaton specifications
 * ldv_08_1a_A.spc, ldv_08_1a.spc */
struct module;

void ldv_module_get(struct module * module) {}
int ldv_try_module_get(struct module * module)
{
  int nondet;
  return nondet;
}
void ldv_module_put(struct module * module) {}
void ldv_module_put_and_exit() {}
int ldv_module_refcount()
{
  int nondet;
  return nondet;
}

void main(void)
{
  struct module * test_module;
  if (ldv_try_module_get(test_module))
  {
    ldv_module_put(test_module);
  }
}
