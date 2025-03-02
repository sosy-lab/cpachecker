// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned long __kernel_ulong_t;
typedef __kernel_ulong_t __kernel_size_t;
typedef __kernel_size_t size_t;

void __VERIFIER_assume(int);

void ldv_reference_free(void *s)
{
  free(s);
  return;
}

void ldv_free(void *s)
{
  ldv_reference_free(s);
  return;
}

long ldv_is_err(void const *ptr)
{
  if ((unsigned long)ptr > 18446744073709547520UL) 
                                                   return 1L; else 
                                                                   return 0L;
}

void *ldv_xmalloc(size_t size)
{
  void *res;
  res = ldv_reference_xmalloc(size);
  __VERIFIER_assume(ldv_is_err((void const *)res) == 0L);
  return res;
}

void *ldv_reference_xmalloc(size_t size)
{
  void *res;
  res = malloc(size);
  __VERIFIER_assume(res != (void *)0);
  return res;
}

size_t ldv_strlen(char const *s)
{
  unsigned int len = 0U;
  goto ldv_1498;
  ldv_1497: 
  ;
  len ++;
  s ++;
  ldv_1498: 
  ;
  if ((int)*s != 0) 
                    goto ldv_1497;
  ldv_1499: 
  ;
  return (unsigned long)len;
}

void ldv_unexpected_error(void)
{
  char tmp = *((char *)0);
  return;
}

static size_t cif_strlen(char const *s)
{
  return ldv_strlen(s);
}

int main(void)
{
  char *str1 = ldv_xmalloc(4UL);
  char *str2 = (char *)"ldv";
  *str1 = (char)108;
  *(str1 + 1U) = (char)100;
  *(str1 + 2U) = (char)118;
  *(str1 + 3U) = (char)0;
  if (cif_strlen((char const *)str1) != 3UL) 
                                             ldv_unexpected_error();
  if (cif_strlen((char const *)str2) != 3UL) 
                                             ldv_unexpected_error();
  ldv_free((void *)str1);
  return 0;
}
