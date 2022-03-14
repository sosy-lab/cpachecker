// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

CONTROL AUTOMATON linux_alloc_usblock
INITIAL STATE Unlocked;

STATE USEALL Unlocked :
  // Lock usb_device.
  MATCH {usb_lock_device($?)} -> GOTO Locked;

  // Lock usb_device if return value is not zero.
  MATCH {$1=usb_trylock_device($?)} -> ASSUME {$1} GOTO Locked;
  // Do not lock usb_device if return value is zero.
  MATCH {$1=usb_trylock_device($?)} -> GOTO Unlocked;

  // Lock usb_device if return value is zero.
  MATCH {$1=usb_lock_device_for_reset($?)} -> ASSUME {!$1} GOTO Locked;
  // Do not lock usb_device if return value is not zero.
  MATCH {$1=usb_lock_device_for_reset($?)} -> GOTO Unlocked;

  // Cut paths, on which usb_device is unlocked before lock.
  MATCH {usb_unlock_device($?)} -> STOP;

STATE USEALL Locked :
  // Unlock usb_device if return value is zero.
  MATCH {usb_unlock_device($?)} -> GOTO Unlocked;

  // Cut paths, on which usb_device is locked twice.
  MATCH {usb_lock_device($?)} -> STOP;

  // Cut paths, on which usb_device is locked twice.
  MATCH {$1=usb_trylock_device($?)} -> STOP;

  // Cut paths, on which usb_device is locked twice.
  MATCH {$1=usb_lock_device_for_reset($?)} -> STOP;

  // Check if flags value ($3) satisfies locked usb_device section.
  MATCH {$1 = kmalloc($2, $3)} -> ASSUME {((int)$3)!=16; ((int)$3)!=32} ERROR("linux:alloc:usb lock::wrong flags");
  // Allow paths, on which flags value ($3) satisfies usb_device locked section.
  MATCH {$1 = kmalloc($2, $3)} -> ASSUME {((int)$3)==32} GOTO Locked;
  MATCH {$1 = kmalloc($2, $3)} -> ASSUME {((int)$3)==16} GOTO Locked;

  // Check for calls, which are forbidden inside locked usb_device section.
  MATCH {$1 = vmalloc($2)} ->  ERROR("linux:alloc:usb lock::nonatomic");

END AUTOMATON
