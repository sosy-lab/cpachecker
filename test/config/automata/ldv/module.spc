// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON linux_module
LOCAL int module_state = 0;
INITIAL STATE Init;

STATE USEALL Init :
  // Increase reference counter if parameter is a valid pointer.
  MATCH {__module_get($1)} -> ASSUME {((struct module *)$1) != 0} DO module_state=1 GOTO Inc;
  // Do nothing if parameter is NULL pointer.
  MATCH {__module_get($1)} -> ASSUME {((struct module *)$1) == 0} GOTO Init;

  // Increase reference counter if parameter is a valid pointer and return value is not 0.
  MATCH {$1=try_module_get($2)} -> ASSUME {((int)$1)!=0; ((struct module *)$2)!=0} DO module_state=1 GOTO Inc;
  // Do nothing if parameter is a valid pointer and return value is 0.
  MATCH {$1=try_module_get($2)} -> ASSUME {((int)$1)==0; ((struct module *)$2)!=0} GOTO Init;
  // Do nothing if parameter is NULL pointer and return value is not 0.
  MATCH {$1=try_module_get($2)} -> ASSUME {((int)$1)!=0; ((struct module *)$2)==0} GOTO Init;
  // Cut paths, on which parameter is NULL pointer and return value is 0.
  MATCH {$1=try_module_get($2)} -> ASSUME {((int)$1)==0; ((struct module *)$2)==0} STOP;

  // Fail if parameter is a valid pointer.
  MATCH {module_put($1)} -> ASSUME {((struct module *)$1)} ERROR("linux:module::less initial decrement");
  // Do nothing if parameter is NULL pointer.
  MATCH {module_put($1)} -> GOTO Init;

  // Fail on this call.
  MATCH {module_put_and_exit($?)} -> ERROR("linux:module::less initial decrement");

  // Cut paths, on which function return value is not 0.
  MATCH {$1 = module_refcount($?)} -> ASSUME {((int)$1)!=0} STOP;
  // Allow paths, on which function return value is 0.
  MATCH {$1 = module_refcount($?)} -> ASSUME {((int)$1)==0} GOTO Init;


STATE USEALL Inc :
  // Increase reference counter if parameter is a valid pointer.
  MATCH {__module_get($1)} -> ASSUME {((struct module *)$1) != 0} DO module_state=module_state+1 GOTO Inc;
  // Do nothing if parameter is NULL pointer.
  MATCH {__module_get($1)} -> ASSUME {((struct module *)$1) == 0} GOTO Inc;

  // Increase reference counter if parameter is a valid pointer and return value is not 0.
  MATCH {$1=try_module_get($2)} -> ASSUME {((int)$1)!=0; ((struct module *)$2)!=0} DO module_state=module_state+1 GOTO Inc;
  // Do nothing if parameter is a valid pointer and return value is 0.
  MATCH {$1=try_module_get($2)} -> ASSUME {((int)$1)==0; ((struct module *)$2)!=0} GOTO Inc;
  // Do nothing if parameter is NULL pointer and return value is not 0.
  MATCH {$1=try_module_get($2)} -> ASSUME {((int)$1)!=0; ((struct module *)$2)==0} GOTO Inc;
  // Cut paths, on which parameter is NULL pointer and return value is 0.
  MATCH {$1=try_module_get($2)} -> ASSUME {((int)$1)==0; ((struct module *)$2)==0} STOP;

  // Decrese reference counter and stay in this state if its value is greater than 0.
  MATCH {module_put($1)} -> ASSUME {((struct module *)$1) != 0; $$module_state >  0} DO module_state=module_state-1 GOTO Inc;
  // Decrese reference counter and return to initial state if its value is 0.
  MATCH {module_put($1)} -> ASSUME {((struct module *)$1) != 0; $$module_state <= 0} DO module_state=module_state-1 GOTO Init;
  // Do nothing if parameter is NULL pointer.
  MATCH {module_put($1)} -> ASSUME {((struct module *)$1) == 0} GOTO Inc;

  // Stop execution on this function call.
  MATCH {module_put_and_exit($?)} -> STOP;

  // Cut paths, on which function return value is not equal to reference counter.
  MATCH {$1 = module_refcount($?)} -> ASSUME {((int)$1)!=((int)$$module_state)} STOP;
  // Allow paths, on which function return value is equal to reference counter.
  MATCH {$1 = module_refcount($?)} -> ASSUME {((int)$1)==((int)$$module_state)} GOTO Inc;

  // Fail on exit from this state.
  MATCH {ldv_check_final_state($?)} -> ERROR("linux:module::more initial at exit");

END AUTOMATON
