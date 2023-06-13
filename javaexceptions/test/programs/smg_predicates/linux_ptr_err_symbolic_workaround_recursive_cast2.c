// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef _Bool bool;
typedef unsigned long __kernel_ulong_t;
typedef __kernel_ulong_t __kernel_size_t;
typedef __kernel_size_t size_t;
void *__devm_regmap_init_i2c(void);
void __VERIFIER_assume(int);
static bool IS_ERR(void const *ptr);
static long PTR_ERR(void const *ptr);
char *r;

void *ldv_reference_malloc(size_t size)
{
  void *res;
  res = malloc(size);
    __VERIFIER_assume(res != (void *)0);
    return res;
}

void *ldv_malloc(size_t size)
{
  void *res;
  res = ldv_reference_malloc(size);
  if (res != (void *)0) {
    __VERIFIER_assume(ldv_is_err((void const *)res) == 0L);
  }
  return res;
}
static int sx9500_probe() {
  void *data = __devm_regmap_init_i2c();
  if (ldv_is_err(data))
    return ldv_ptr_err(data);
  r = ldv_malloc(2);
  return 0;
}

int main() {
  int emg_7_probe_retval = sx9500_probe();
  if (emg_7_probe_retval == 0) {
    r[1] = 1;
    free(r);
  } else {
    //unreached path
    r = ldv_malloc(2);
  }
}

long ldv_is_err(void const *ptr)
{
  if ((unsigned long)(unsigned char)(long)ptr > 1000UL) {
    return 1;
  } else {
    return 0;
  }
}

long ldv_ptr_err(void const *ptr)
{
  __VERIFIER_assume((unsigned long)ptr > 1000UL);
  long result = (long)(1000UL - (unsigned long)(long)(char*)ptr);
  __VERIFIER_assume(result != 0);
  return result;
}
