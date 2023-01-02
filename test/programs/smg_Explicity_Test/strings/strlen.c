// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned long size_t;
typedef unsigned long __kernel_ulong_t;
typedef __kernel_ulong_t __kernel_size_t;

size_t cif_strlen(char *s)
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

int cif_strcmp(char *cs, char *ct)
{
  goto ldv_1506;
 ldv_1505: 
  ;
  if ((int)*cs != (int)*ct) 
    goto ldv_1504;
  cs ++;
  ct ++;
 ldv_1506: 
  ;
  if ((int)*cs != 0) 
    goto ldv_1505;
 ldv_1504: 
  ;
  return (int)*cs - (int)*ct;
}

int cif_strncmp(char *cs, char *ct, __kernel_size_t count)
{
  if (count == 0UL) 
    return 0;
  goto ldv_1514;
 ldv_1513: 
  ;
  if ((int)*cs != (int)*ct) 
    goto ldv_1512;
  count --;
  if (count == 0UL) 
    goto ldv_1512;
  cs ++;
  ct ++;
 ldv_1514: 
  ;
  if ((int)*cs != 0) 
    goto ldv_1513;
 ldv_1512: 
  ;
  return (int)*cs - (int)*ct;
}

int ldv_memcmp(void *cs, void *ct, size_t count)
{
  unsigned char *su1;
  unsigned char *su2;
  int res = 0;
  su1 = (unsigned char *)cs;
  su2 = (unsigned char *)ct;
  goto ldv_1525;
  ldv_1524:
  ;
  res = (int)*su1 - (int)*su2;
  if (res != 0)
                goto ldv_1523;
  su1 ++;
  su2 ++;
  count --;
  ldv_1525:
  ;
  if (count != 0UL)
                    goto ldv_1524;
  ldv_1523:
  ;
  return res;
}

char *ldv_strstr(char *cs, char *ct)
{
  size_t cs_len;
  size_t ct_len;
  cs_len = cif_strlen(cs);
  ct_len = cif_strlen(ct);
  goto ldv_1533;
  ldv_1532:
  ;
  if (ldv_memcmp((void *)cs,(void *)ct,ct_len) == 0)
                                                     return cs;
  cs_len --;
  cs ++;
  ldv_1533:
  ;
  if (cs_len >= ct_len)
                        goto ldv_1532;
  ldv_1534:
  ;
  return (char *)0;
}

static char *cif_strstr(char *s1, char *s2)
{
  return ldv_strstr(s1,s2);
}

void ldv_unexpected_memory_safety_error(void)
{
  int *var = (int *)0;
  int tmp = *var;
  return;
}

int main(void)
{
  char *s = (char *)"LDV string";
  if (cif_strlen((char *)"LDV string") != 10UL) 
    ldv_unexpected_memory_safety_error();
  if (cif_strlen(s) != 10UL) 
    ldv_unexpected_memory_safety_error();
  if (cif_strcmp(s,(char *)"LDV string") != 0) 
    ldv_unexpected_memory_safety_error();
  if (cif_strcmp(s,(char *)"LDV substring") > 0) 
    ldv_unexpected_memory_safety_error();
  if (cif_strcmp(s,(char *)"LDV") < 0) 
    ldv_unexpected_memory_safety_error();
  if (cif_strncmp(s,(char *)"LDV string",10UL) != 0) 
    ldv_unexpected_memory_safety_error();
  if (cif_strncmp(s,(char *)"LDV substring",5UL) != 0) 
    ldv_unexpected_memory_safety_error();
  if (cif_strncmp(s,(char *)"LDV substring",6UL) > 0) 
    ldv_unexpected_memory_safety_error();
  if (cif_strncmp(s,(char *)"LDV substring",7UL) > 0) 
    ldv_unexpected_memory_safety_error();
  if (cif_strncmp(s,(char *)"LDV",10UL) < 0) 
    ldv_unexpected_memory_safety_error();
  if (cif_strcmp(cif_strstr(s,(char *)"str"),(char *)"string") != 0) 
    ldv_unexpected_memory_safety_error();
  if (cif_strstr(s,(char *)"Klever") != (char *)0) 
    ldv_unexpected_memory_safety_error();
  return 0;
}
