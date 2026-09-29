// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Unsafe: probe, suspend, resume, pre_reset, reset_resume locks an already locked mutex.
// Reduced from the Synaptics benchmark; there are no pointer or heap operations.
// With fine block boundaries, replacement of conditions can stall before this sequence.

extern void abort(void);
extern int __VERIFIER_nondet_int(void);
extern void __VERIFIER_error(void);
int ldv_mutex = 1;
int LDV_IN_INTERRUPT;
static int res_synusb_probe_7;
void ldv_blast_assert(void) { __VERIFIER_error(); abort(); }
void mutex_lock(int lock) { if (ldv_mutex != 1) ldv_blast_assert(); ldv_mutex = 2; }
void mutex_unlock(int lock) { if (ldv_mutex != 2) ldv_blast_assert(); ldv_mutex = 1; }
int synusb_probe(int intf, int id) { return 0; }
void synusb_disconnect(int intf) { }
int synusb_suspend(int intf, int event) { mutex_lock(0); mutex_unlock(0); return 0; }
int synusb_resume(int intf) { mutex_lock(0); mutex_unlock(0); return 0; }
int synusb_pre_reset(int intf) { mutex_lock(0); return 0; }
int synusb_post_reset(int intf) { mutex_unlock(0); return 0; }
int synusb_reset_resume(int intf) { int result = synusb_resume(intf); return result; }
void ldv_check_return_value(int res) { }
void ldv_initialize(void) { }
void ldv_check_final_state(void) { if (ldv_mutex != 1) ldv_blast_assert(); }
int main(void)
{ int var_group1 = 0 ;
  int var_synusb_probe_7_p1 = 0 ;
  int var_synusb_suspend_9_p1 ;
  int ldv_s_synusb_driver_usb_driver ;
  int tmp___7 ;
  int tmp___8 ;
  int __cil_tmp7 ;
  int var_synusb_suspend_9_p1_event8 ;
  {
  {
  LDV_IN_INTERRUPT = 1;
  ldv_initialize();
  ldv_s_synusb_driver_usb_driver = 0;
  }
  {
  while (1) {
    while_continue: ;
    {
    tmp___8 = __VERIFIER_nondet_int();
    }
    if (tmp___8) {
    } else {
      {
      __cil_tmp7 = ldv_s_synusb_driver_usb_driver == 0;
      if (! __cil_tmp7) {
      } else {
        goto while_break;
      }
      }
    }
    {
    tmp___7 = __VERIFIER_nondet_int();
    }
    if (tmp___7 == 0) {
      goto case_0;
    } else
    if (tmp___7 == 1) {
      goto case_1;
    } else
    if (tmp___7 == 2) {
      goto case_2;
    } else
    if (tmp___7 == 3) {
      goto case_3;
    } else
    if (tmp___7 == 4) {
      goto case_4;
    } else
    if (tmp___7 == 5) {
      goto case_5;
    } else
    if (tmp___7 == 6) {
      goto case_6;
    } else {
      {
      goto switch_default;
      if (0) {
        case_0:
        if (ldv_s_synusb_driver_usb_driver == 0) {
          {
          res_synusb_probe_7 = synusb_probe(var_group1, var_synusb_probe_7_p1);
          ldv_check_return_value(res_synusb_probe_7);
          }
          if (res_synusb_probe_7) {
            goto ldv_module_exit;
          } else {
          }
          ldv_s_synusb_driver_usb_driver = ldv_s_synusb_driver_usb_driver + 1;
        } else {
        }
        goto switch_break;
        case_1:
        if (ldv_s_synusb_driver_usb_driver == 1) {
          {
          synusb_suspend(var_group1, var_synusb_suspend_9_p1_event8);
          ldv_s_synusb_driver_usb_driver = ldv_s_synusb_driver_usb_driver + 1;
          }
        } else {
        }
        goto switch_break;
        case_2:
        if (ldv_s_synusb_driver_usb_driver == 2) {
          {
          synusb_resume(var_group1);
          ldv_s_synusb_driver_usb_driver = ldv_s_synusb_driver_usb_driver + 1;
          }
        } else {
        }
        goto switch_break;
        case_3:
        if (ldv_s_synusb_driver_usb_driver == 3) {
          {
          synusb_pre_reset(var_group1);
          ldv_s_synusb_driver_usb_driver = ldv_s_synusb_driver_usb_driver + 1;
          }
        } else {
        }
        goto switch_break;
        case_4:
        if (ldv_s_synusb_driver_usb_driver == 4) {
          {
          synusb_reset_resume(var_group1);
          ldv_s_synusb_driver_usb_driver = ldv_s_synusb_driver_usb_driver + 1;
          }
        } else {
        }
        goto switch_break;
        case_5:
        if (ldv_s_synusb_driver_usb_driver == 5) {
          {
          synusb_post_reset(var_group1);
          ldv_s_synusb_driver_usb_driver = ldv_s_synusb_driver_usb_driver + 1;
          }
        } else {
        }
        goto switch_break;
        case_6:
        if (ldv_s_synusb_driver_usb_driver == 6) {
          {
          synusb_disconnect(var_group1);
          ldv_s_synusb_driver_usb_driver = 0;
          }
        } else {
        }
        goto switch_break;
        switch_default:
        goto switch_break;
      } else {
        switch_break: ;
      }
      }
    }
  }
  while_break: ;
  }
  ldv_module_exit:
  {
  ldv_check_final_state();
  }
  return 0;
}
}
