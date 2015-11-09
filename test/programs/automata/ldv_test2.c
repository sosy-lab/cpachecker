/* Test file for automaton cpa. Use with automaton specification
 * test2.spc */
void ldv_module_get(void) {}
void ldv_module_put(void) {}
void ldv_check_final_state(void) {}

void main(void)
{
  // [Init] state == 0
  ldv_module_get(); 
  // [Inc] state == 1
  ldv_module_get(); 
  // [Inc] state == 2
  ldv_module_put(); 
  // -- split -- [Inc] state == 1
  //             [Init] state == 1
  ldv_module_put(); // 0
  ldv_check_final_state();
}
