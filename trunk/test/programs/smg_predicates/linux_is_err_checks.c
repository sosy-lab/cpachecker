// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef _Bool bool;

struct list_head {
   struct list_head *next ;
   struct list_head *prev ;
};

struct list_head *iget_locked(void);

void __VERIFIER_assume(int);

void *ldv_err_ptr(long error)
{
  unsigned long result;
  __VERIFIER_assume(error < 0L);
  __VERIFIER_assume(error >= -4095L);
  result = 18446744073709547520UL - (unsigned long)error;
  __VERIFIER_assume(result > 18446744073709547520UL);
  return (void *)result;
}

static void *ERR_PTR(long error)
{
  return ldv_err_ptr(error);
}

long ldv_is_err(void const *ptr)
{
  if ((unsigned long)ptr > 18446744073709547520UL)
    return 1L;
  else
    return 0L;
}

static bool IS_ERR(void const *ptr)
{
  long ret;
  ret = ldv_is_err(ptr);
  return (_Bool)(ret != 0L);
}

int main()
{
  struct list_head *inode;
  int ret = 0;
  struct list_head *s;
  inode = iget_locked();
  
  if (inode == (struct list *)0) {
    inode = (struct list *)ERR_PTR(-12L);
  }

  if ((int)IS_ERR((void const *)inode) != 0) {
    if (IS_ERR((void const *)inode) == 0) {
      s = 0;
      s->next = 0;
      ret = (int)PTR_ERR((void const *)inode);
    }
  }
  return ret;
}

