// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON linux_class
INITIAL STATE G0_C0_H0;

STATE USEALL G0_C0_H0 :
  // Cut paths, on which function returns 0.
  MATCH {$1=__class_create($?)} -> ASSUME {((void *)$1) == 0} STOP;
  // Do nothing if function returns ERR_PTR.
  MATCH {$1=__class_create($?)} -> ASSUME {((void *)$1) > 2012} GOTO G0_C0_H0;
  // Create class structure.
  MATCH {$1=__class_create($?)} -> ASSUME {((void *)$1) <= 2012; ((int)$1) > 0} GOTO G0_C1_H0;

  // Create class structure on 0 return value.
  MATCH {$1=class_interface_register($?)} -> ASSUME {((int)$1) == 0} GOTO G0_C1_H0;
  // Do nothing on negative return value.
  MATCH {$1=class_interface_register($?)} -> ASSUME {((int)$1) <  0} GOTO G0_C0_H0;
  // Cut paths, on which function returns positive value.
  MATCH {$1=class_interface_register($?)} -> ASSUME {((int)$1) >  0} STOP;

  // Class was not created.
  MATCH {class_interface_unregister($?)} -> ERROR("linux:class::double deregistration");

  // Do nothing on NULL pointer.
  MATCH {class_destroy($1)} -> ASSUME {((void *)$1) == 0} GOTO G0_C0_H0;
  // Do nothing if function gets ERR_PTR.
  MATCH {class_destroy($1)} -> ASSUME {((void *)$1) > 2012} GOTO G0_C0_H0;
  // Class was not created and pointer is valid.
  MATCH {class_destroy($1)} -> ASSUME {((void *)$1) <= 2012; ((void *)$1) > 0} ERROR("linux:class::double deregistration");

STATE USEALL G0_C1_H0 :
  // Cut paths, on which function returns 0.
  MATCH {$1=__class_create($?)} -> ASSUME {((void *)$1) == 0} STOP;
  // Do nothing if function returns ERR_PTR.
  MATCH {$1=__class_create($?)} -> ASSUME {((void *)$1) > 2012} GOTO G0_C1_H0;
  // Create class twice.
  MATCH {$1=__class_create($?)} -> ASSUME {((void *)$1) <= 2012; ((int)$1) > 0} ERROR("linux:class::double registration");

  // Create class twice.
  MATCH {$1=class_interface_register($?)} -> ASSUME {((int)$1) == 0} ERROR("linux:class::double registration");

  MATCH {$1=class_interface_register($?)} -> ASSUME {((int)$1) <  0} GOTO G0_C1_H0;
  // Cut paths, on which function returns positive value.
  MATCH {$1=class_interface_register($?)} -> ASSUME {((int)$1) >  0} STOP;

  // Unregister class.
  MATCH {class_interface_unregister($?)} -> GOTO G0_C0_H0;

  // Do nothing on NULL pointer.
  MATCH {class_destroy($1)} -> ASSUME {((void *)$1) == 0} GOTO G0_C1_H0;
  // Do nothing if function gets ERR_PTR.
  MATCH {class_destroy($1)} -> ASSUME {((void *)$1) > 2012} GOTO G0_C1_H0;
  // Destroy class structure.
  MATCH {class_destroy($1)} -> ASSUME {((void *)$1) <= 2012; ((void *)$1) > 0} GOTO G0_C0_H0;

  // Should not be in this state on exit.
  MATCH {ldv_check_final_state($?)} -> ERROR("linux:class::registered at exit");

END AUTOMATON

