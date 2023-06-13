// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void *ERR_PTR(long error) {
  void *err_ptr;
  err_ptr = ldv_err_ptr(error);
  return err_ptr;
}

void *ldv_err_ptr(long error) {
  void *ldv_err_ptr;
  ldv_err_ptr = (void *)(4294967295L - error);
  return ldv_err_ptr;
}

long int IS_ERR(const void *ptr) {
  long is_err;
  is_err = ldv_is_err(ptr);
  return is_err;
}

long int ldv_is_err(const void *ptr) {
  long int ldv_is_err;
  ldv_is_err = ((unsigned long)ptr) > 4294967295UL;
  return ldv_is_err;
}


void main() {
  void *tmp;
  int res;
  tmp = ERR_PTR(-12);
  res = IS_ERR(tmp);
  if (res != 0) {
    free(tmp);
    return;
  }
  tmp = ERR_PTR(-12);
  res = IS_ERR(tmp);
  if (res) {
    free(tmp);
  }
}
