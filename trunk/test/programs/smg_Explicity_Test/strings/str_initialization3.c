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

int is_eol(char const *s, size_t size)
{
  if (s[size] == 0)
    return 1;
  else
    return 0;
}

int main(void)
{
  char *str2;
  int r;
  str2 = "ldv";
  if (cif_strlen((char const *)str2) != 3UL)
    ldv_unexpected_error();
  r = is_eol(str2, 4); // ERROR
  return 0;
}
