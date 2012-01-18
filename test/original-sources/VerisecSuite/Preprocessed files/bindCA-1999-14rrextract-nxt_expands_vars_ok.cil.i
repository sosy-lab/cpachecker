# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_expands_vars_ok.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_expands_vars_ok.cil.c"
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
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
extern int ( nondet_short)() ;
# 32 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
extern int ( nondet_long)() ;
# 6 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
static int rrextract(u_char *msg , int msglen , u_char *rrp , u_char *dname , int namelen )
{ u_char *eom ;
  u_char *cp ;
  u_char *cp1 ;
  u_char *rdatap ;
  u_int class ;
  u_int type ;
  u_int dlen ;
  int n ;
  int n1 ;
  int n2 ;
  u_int32_t ttl ;
  u_char data[14] ;
  int tmp ;
  int tmp___0 ;
  unsigned int tmp___1 ;
  unsigned int tmp___2 ;
  unsigned long __cil_tmp22 ;
  unsigned long __cil_tmp23 ;
  u_char const *__cil_tmp24 ;
  u_char const *__cil_tmp25 ;
  u_char const *__cil_tmp26 ;
  unsigned long __cil_tmp27 ;
  u_char *__cil_tmp28 ;
  unsigned long __cil_tmp29 ;
  unsigned long __cil_tmp30 ;
  u_char *__cil_tmp31 ;
  unsigned long __cil_tmp32 ;
  u_char const *__cil_tmp33 ;
  u_char const *__cil_tmp34 ;
  u_char const *__cil_tmp35 ;
  unsigned long __cil_tmp36 ;
  unsigned long __cil_tmp37 ;
  u_char *__cil_tmp38 ;
  int __cil_tmp39 ;
  unsigned long __cil_tmp40 ;
  unsigned long __cil_tmp41 ;
  u_char *__cil_tmp42 ;
  char const *__cil_tmp43 ;
  unsigned int __cil_tmp44 ;
  unsigned long __cil_tmp45 ;
  unsigned long __cil_tmp46 ;
  u_char *__cil_tmp47 ;
  unsigned long __cil_tmp48 ;
  unsigned long __cil_tmp49 ;
  unsigned long __cil_tmp50 ;
  unsigned long __cil_tmp51 ;
  unsigned long __cil_tmp52 ;
  u_char *__cil_tmp53 ;
  char const *__cil_tmp54 ;
  unsigned int __cil_tmp55 ;
  unsigned long __cil_tmp56 ;
  unsigned long __cil_tmp57 ;
  u_int __cil_tmp58 ;
  unsigned long __cil_tmp59 ;
  void *__cil_tmp60 ;
  void const *__cil_tmp61 ;

  {
  {
# 15 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp22 = 13 * 1UL;
# 15 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp23 = (unsigned long )(data) + __cil_tmp22;
# 15 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  *((u_char *)__cil_tmp23) = (char)0;
# 17 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  cp = rrp;
# 18 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  eom = msg + msglen;
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp24 = (u_char const *)msg;
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp25 = (u_char const *)eom;
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp26 = (u_char const *)cp;
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  n = dn_expand(__cil_tmp24, __cil_tmp25, __cil_tmp26, dname, namelen);
  }
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  if (n < 0) {
# 21 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    return (-1);
  } else {

  }
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  cp = cp + n;
  {
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  while (1) {
    while_0_continue: ;
    {
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    __cil_tmp27 = (unsigned long )eom;
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    __cil_tmp28 = cp + 10;
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    __cil_tmp29 = (unsigned long )__cil_tmp28;
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    if (__cil_tmp29 > __cil_tmp27) {
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
      return (-1);
    } else {

    }
    }
    goto while_0_break;
  }
  while_0_break: ;
  }
  {
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  while (1) {
    while_1_continue: ;
    {
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    type = nondet_short();
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    cp = cp + 2;
    }
    goto while_1_break;
  }
  while_1_break: ;
  }
  {
# 27 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  while (1) {
    while_2_continue: ;
    {
# 27 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    class = nondet_short();
# 27 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    cp = cp + 2;
    }
    goto while_2_break;
  }
  while_2_break: ;
  }
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  if (class > 100) {
# 30 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    return (-1);
  } else {

  }
  {
# 32 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  while (1) {
    while_3_continue: ;
    {
# 32 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    ttl = nondet_long();
# 32 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    cp = cp + 4;
    }
    goto while_3_break;
  }
  while_3_break: ;
  }
# 34 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  if (ttl > 101) {
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    ttl = 0;
  } else {

  }
  {
# 37 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  while (1) {
    while_4_continue: ;
    {
# 37 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    dlen = nondet_short();
# 37 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    cp = cp + 2;
    }
    goto while_4_break;
  }
  while_4_break: ;
  }
  {
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  while (1) {
    while_5_continue: ;
    {
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    __cil_tmp30 = (unsigned long )eom;
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    __cil_tmp31 = cp + dlen;
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    __cil_tmp32 = (unsigned long )__cil_tmp31;
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    if (__cil_tmp32 > __cil_tmp30) {
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
      return (-1);
    } else {

    }
    }
    goto while_5_break;
  }
  while_5_break: ;
  }
  {
# 40 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  rdatap = cp;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  tmp = nondet_int();
  }
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  if (tmp) {
# 43 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    return (-1);
  } else {

  }
  {
# 49 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp33 = (u_char const *)msg;
# 49 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp34 = (u_char const *)eom;
# 49 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp35 = (u_char const *)cp;
# 49 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp36 = 0 * 1UL;
# 49 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp37 = (unsigned long )(data) + __cil_tmp36;
# 49 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp38 = (u_char *)__cil_tmp37;
# 49 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp39 = (int )14UL;
# 49 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  n = dn_expand(__cil_tmp33, __cil_tmp34, __cil_tmp35, __cil_tmp38, __cil_tmp39);
  }
# 51 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  if (n < 0) {
# 52 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    return (-1);
  } else {
# 51 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    if (n >= dlen) {
# 52 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
      return (-1);
    } else {

    }
  }
  {
# 55 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  tmp___0 = nondet_int();
  }
# 55 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  if (tmp___0) {
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    return (-1);
  } else {

  }
  {
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  cp = cp + n;
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp40 = 0 * 1UL;
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp41 = (unsigned long )(data) + __cil_tmp40;
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp42 = (u_char *)__cil_tmp41;
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp43 = (char const *)__cil_tmp42;
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  tmp___1 = strlen(__cil_tmp43);
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp44 = tmp___1 + 1U;
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  n1 = (int )__cil_tmp44;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp45 = 0 * 1UL;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp46 = (unsigned long )(data) + __cil_tmp45;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp47 = (u_char *)__cil_tmp46;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  cp1 = __cil_tmp47 + n1;
# 63 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  n2 = dlen - n;
  }
  {
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp48 = (unsigned long )n1;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp49 = 14UL - __cil_tmp48;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp50 = (unsigned long )n2;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  if (__cil_tmp50 > __cil_tmp49) {
# 65 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    return (-1);
  } else {

  }
  }
  {
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp51 = 0 * 1UL;
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp52 = (unsigned long )(data) + __cil_tmp51;
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp53 = (u_char *)__cil_tmp52;
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp54 = (char const *)__cil_tmp53;
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  tmp___2 = strlen(__cil_tmp54);
  }
  {
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp55 = tmp___2 + 1U;
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp56 = (unsigned long )__cil_tmp55;
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp57 = 14UL - __cil_tmp56;
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp58 = dlen - n;
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp59 = (unsigned long )__cil_tmp58;
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  if (__cil_tmp59 <= __cil_tmp57) {

  } else {
    {
# 68 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
    __assert_fail("dlen - n <= sizeof data - (strlen((char *)data) + 1)", "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c",
                  68U, "rrextract");
    }
  }
  }
  {
# 71 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp60 = (void *)cp1;
# 71 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp61 = (void const *)cp;
# 71 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  r_memcpy(__cil_tmp60, __cil_tmp61, n2);
  }
# 73 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  return (0);
}
}
# 76 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
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
# 83 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp6 = 2 * 1UL;
# 83 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp7 = (unsigned long )(name) + __cil_tmp6;
# 83 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  *((u_char *)__cil_tmp7) = (char)0;
# 84 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp8 = 13 * 1UL;
# 84 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp9 = (unsigned long )(msg) + __cil_tmp8;
# 84 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  *((u_char *)__cil_tmp9) = (char)0;
# 86 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  msglen = 4;
# 87 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp10 = 0 * 1UL;
# 87 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp11 = (unsigned long )(msg) + __cil_tmp10;
# 87 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  dp = (u_char *)__cil_tmp11;
# 89 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp12 = 0 * 1UL;
# 89 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp13 = (unsigned long )(msg) + __cil_tmp12;
# 89 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp14 = (u_char *)__cil_tmp13;
# 89 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp15 = 0 * 1UL;
# 89 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp16 = (unsigned long )(name) + __cil_tmp15;
# 89 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  __cil_tmp17 = (u_char *)__cil_tmp16;
# 89 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  ret = rrextract(__cil_tmp14, msglen, dp, __cil_tmp17, 3);
  }
# 91 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_expands_vars_ok.c"
  return (0);
}
}
