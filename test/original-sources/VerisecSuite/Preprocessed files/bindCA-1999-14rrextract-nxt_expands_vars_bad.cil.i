# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_expands_vars_bad.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_expands_vars_bad.cil.c"
# 14 "../versisec/bind/progs1/../lib/stubs.h"
typedef int size_t;
# 7 "../versisec/bind/progs1/../bind.h"
typedef char u_char;
# 8 "../versisec/bind/progs1/../bind.h"
typedef int u_int;
# 9 "../versisec/bind/progs1/../bind.h"
typedef int u_int32_t;
# 27 "../versisec/bind/progs1/../lib/stubs.h"
extern unsigned int strlen(char const *s ) ;
# 53 "../versisec/bind/progs1/../lib/stubs.h"
extern void *r_memcpy(void *dest , void const *src , size_t n ) ;
# 46 "../versisec/bind/progs1/../bind.h"
extern int dn_expand(u_char const *msg , u_char const *eomorig , u_char const *comp_dn ,
                     char *exp_dn , int length ) ;
# 50 "../versisec/bind/progs1/../bind.h"
extern int nondet_int() ;
# 71 "/usr/include/assert.h"
extern __attribute__((__nothrow__, __noreturn__)) void __assert_fail(char const *__assertion ,
                                                                      char const *__file ,
                                                                      unsigned int __line ,
                                                                      char const *__function ) ;
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
extern int ( nondet_short)() ;
# 31 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
extern int ( nondet_long)() ;
# 6 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
static int rrextract(u_char *msg , int msglen , u_char *rrp , u_char *dname , int namelen )
{ u_char *eom ;
  u_char *cp ;
  u_char *cp1 ;
  u_char *rdatap ;
  u_int class ;
  u_int type ;
  u_int dlen ;
  int n ;
  u_int32_t ttl ;
  u_char data[14] ;
  int tmp ;
  int tmp___0 ;
  unsigned int tmp___1 ;
  unsigned int tmp___2 ;
  unsigned long __cil_tmp20 ;
  unsigned long __cil_tmp21 ;
  u_char const *__cil_tmp22 ;
  u_char const *__cil_tmp23 ;
  u_char const *__cil_tmp24 ;
  unsigned long __cil_tmp25 ;
  u_char *__cil_tmp26 ;
  unsigned long __cil_tmp27 ;
  unsigned long __cil_tmp28 ;
  u_char *__cil_tmp29 ;
  unsigned long __cil_tmp30 ;
  u_char const *__cil_tmp31 ;
  u_char const *__cil_tmp32 ;
  u_char const *__cil_tmp33 ;
  unsigned long __cil_tmp34 ;
  unsigned long __cil_tmp35 ;
  u_char *__cil_tmp36 ;
  int __cil_tmp37 ;
  unsigned long __cil_tmp38 ;
  unsigned long __cil_tmp39 ;
  u_char *__cil_tmp40 ;
  char const *__cil_tmp41 ;
  unsigned long __cil_tmp42 ;
  unsigned long __cil_tmp43 ;
  u_char *__cil_tmp44 ;
  u_char *__cil_tmp45 ;
  unsigned long __cil_tmp46 ;
  unsigned long __cil_tmp47 ;
  u_char *__cil_tmp48 ;
  char const *__cil_tmp49 ;
  unsigned int __cil_tmp50 ;
  unsigned long __cil_tmp51 ;
  unsigned long __cil_tmp52 ;
  u_int __cil_tmp53 ;
  unsigned long __cil_tmp54 ;
  void *__cil_tmp55 ;
  void const *__cil_tmp56 ;
  u_int __cil_tmp57 ;

  {
  {
# 14 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp20 = 13 * 1UL;
# 14 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp21 = (unsigned long )(data) + __cil_tmp20;
# 14 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  *((u_char *)__cil_tmp21) = (char)0;
# 16 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  cp = rrp;
# 17 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  eom = msg + msglen;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp22 = (u_char const *)msg;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp23 = (u_char const *)eom;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp24 = (u_char const *)cp;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  n = dn_expand(__cil_tmp22, __cil_tmp23, __cil_tmp24, dname, namelen);
  }
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  if (n < 0) {
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    return (-1);
  } else {

  }
# 23 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  cp = cp + n;
  {
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  while (1) {
    while_0_continue: ;
    {
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    __cil_tmp25 = (unsigned long )eom;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    __cil_tmp26 = cp + 10;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    __cil_tmp27 = (unsigned long )__cil_tmp26;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    if (__cil_tmp27 > __cil_tmp25) {
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
      return (-1);
    } else {

    }
    }
    goto while_0_break;
  }
  while_0_break: ;
  }
  {
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  while (1) {
    while_1_continue: ;
    {
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    type = nondet_short();
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    cp = cp + 2;
    }
    goto while_1_break;
  }
  while_1_break: ;
  }
  {
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  while (1) {
    while_2_continue: ;
    {
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    class = nondet_short();
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    cp = cp + 2;
    }
    goto while_2_break;
  }
  while_2_break: ;
  }
# 28 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  if (class > 100) {
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    return (-1);
  } else {

  }
  {
# 31 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  while (1) {
    while_3_continue: ;
    {
# 31 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    ttl = nondet_long();
# 31 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    cp = cp + 4;
    }
    goto while_3_break;
  }
  while_3_break: ;
  }
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  if (ttl > 101) {
# 34 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    ttl = 0;
  } else {

  }
  {
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  while (1) {
    while_4_continue: ;
    {
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    dlen = nondet_short();
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    cp = cp + 2;
    }
    goto while_4_break;
  }
  while_4_break: ;
  }
  {
# 37 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  while (1) {
    while_5_continue: ;
    {
# 37 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    __cil_tmp28 = (unsigned long )eom;
# 37 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    __cil_tmp29 = cp + dlen;
# 37 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    __cil_tmp30 = (unsigned long )__cil_tmp29;
# 37 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    if (__cil_tmp30 > __cil_tmp28) {
# 37 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
      return (-1);
    } else {

    }
    }
    goto while_5_break;
  }
  while_5_break: ;
  }
  {
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  rdatap = cp;
# 41 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  tmp = nondet_int();
  }
# 41 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  if (tmp) {
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    return (-1);
  } else {

  }
  {
# 48 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp31 = (u_char const *)msg;
# 48 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp32 = (u_char const *)eom;
# 48 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp33 = (u_char const *)cp;
# 48 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp34 = 0 * 1UL;
# 48 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp35 = (unsigned long )(data) + __cil_tmp34;
# 48 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp36 = (u_char *)__cil_tmp35;
# 48 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp37 = (int )14UL;
# 48 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  n = dn_expand(__cil_tmp31, __cil_tmp32, __cil_tmp33, __cil_tmp36, __cil_tmp37);
  }
# 50 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  if (n < 0) {
# 51 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    return (-1);
  } else {
# 50 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    if (n >= dlen) {
# 51 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
      return (-1);
    } else {

    }
  }
  {
# 54 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  tmp___0 = nondet_int();
  }
# 54 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  if (tmp___0) {
# 55 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    return (-1);
  } else {

  }
  {
# 57 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  cp = cp + n;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp38 = 0 * 1UL;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp39 = (unsigned long )(data) + __cil_tmp38;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp40 = (u_char *)__cil_tmp39;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp41 = (char const *)__cil_tmp40;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  tmp___1 = strlen(__cil_tmp41);
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp42 = 0 * 1UL;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp43 = (unsigned long )(data) + __cil_tmp42;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp44 = (u_char *)__cil_tmp43;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp45 = __cil_tmp44 + tmp___1;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  cp1 = __cil_tmp45 + 1;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp46 = 0 * 1UL;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp47 = (unsigned long )(data) + __cil_tmp46;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp48 = (u_char *)__cil_tmp47;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp49 = (char const *)__cil_tmp48;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  tmp___2 = strlen(__cil_tmp49);
  }
  {
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp50 = tmp___2 + 1U;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp51 = (unsigned long )__cil_tmp50;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp52 = 14UL - __cil_tmp51;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp53 = dlen - n;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp54 = (unsigned long )__cil_tmp53;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  if (__cil_tmp54 <= __cil_tmp52) {

  } else {
    {
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
    __assert_fail("dlen - n <= sizeof data - (strlen((char *)data) + 1)", "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c",
                  61U, "rrextract");
    }
  }
  }
  {
# 63 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp55 = (void *)cp1;
# 63 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp56 = (void const *)cp;
# 63 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp57 = dlen - n;
# 63 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  r_memcpy(__cil_tmp55, __cil_tmp56, __cil_tmp57);
  }
# 65 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  return (0);
}
}
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
int main(void)
{ int msglen ;
  int ret ;
  u_char *dp ;
  u_char name[3] ;
  u_char msg[14] ;
  unsigned long __cil_tmp6 ;
  unsigned long __cil_tmp7 ;
  unsigned long __cil_tmp8 ;
  unsigned long __cil_tmp9 ;
  unsigned long __cil_tmp10 ;
  unsigned long __cil_tmp11 ;
  unsigned long __cil_tmp12 ;
  unsigned long __cil_tmp13 ;
  u_char *__cil_tmp14 ;
  unsigned long __cil_tmp15 ;
  unsigned long __cil_tmp16 ;
  u_char *__cil_tmp17 ;

  {
  {
# 75 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp6 = 2 * 1UL;
# 75 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp7 = (unsigned long )(name) + __cil_tmp6;
# 75 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  *((u_char *)__cil_tmp7) = (char)0;
# 76 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp8 = 13 * 1UL;
# 76 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp9 = (unsigned long )(msg) + __cil_tmp8;
# 76 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  *((u_char *)__cil_tmp9) = (char)0;
# 78 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  msglen = 4;
# 79 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp10 = 0 * 1UL;
# 79 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp11 = (unsigned long )(msg) + __cil_tmp10;
# 79 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  dp = (u_char *)__cil_tmp11;
# 81 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp12 = 0 * 1UL;
# 81 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp13 = (unsigned long )(msg) + __cil_tmp12;
# 81 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp14 = (u_char *)__cil_tmp13;
# 81 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp15 = 0 * 1UL;
# 81 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp16 = (unsigned long )(name) + __cil_tmp15;
# 81 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  __cil_tmp17 = (u_char *)__cil_tmp16;
# 81 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  ret = rrextract(__cil_tmp14, msglen, dp, __cil_tmp17, 3);
  }
# 83 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_bad.c"
  return (0);
}
}
