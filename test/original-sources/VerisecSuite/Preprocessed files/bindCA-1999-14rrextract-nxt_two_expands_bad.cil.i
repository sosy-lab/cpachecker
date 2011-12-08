# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_two_expands_bad.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_two_expands_bad.cil.c"
# 14 "../versisec/bind/progs1/../lib/stubs.h"
typedef int size_t;
# 7 "../versisec/bind/progs1/../bind.h"
typedef char u_char;
# 8 "../versisec/bind/progs1/../bind.h"
typedef int u_int;
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
# 18 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
extern int ( nondet_short)() ;
# 6 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
static int rrextract(u_char *msg , int msglen , u_char *rrp , u_char *dname , int namelen )
{ u_char *eom ;
  u_char *cp ;
  u_char *cp1 ;
  u_int dlen ;
  int n ;
  u_char data[6] ;
  int tmp ;
  unsigned int tmp___0 ;
  unsigned int tmp___1 ;
  unsigned long __cil_tmp18 ;
  unsigned long __cil_tmp19 ;
  unsigned long __cil_tmp20 ;
  u_char *__cil_tmp21 ;
  unsigned long __cil_tmp22 ;
  u_char const *__cil_tmp23 ;
  u_char const *__cil_tmp24 ;
  u_char const *__cil_tmp25 ;
  u_char const *__cil_tmp26 ;
  u_char const *__cil_tmp27 ;
  u_char const *__cil_tmp28 ;
  unsigned long __cil_tmp29 ;
  unsigned long __cil_tmp30 ;
  u_char *__cil_tmp31 ;
  int __cil_tmp32 ;
  unsigned long __cil_tmp33 ;
  unsigned long __cil_tmp34 ;
  u_char *__cil_tmp35 ;
  char const *__cil_tmp36 ;
  unsigned long __cil_tmp37 ;
  unsigned long __cil_tmp38 ;
  u_char *__cil_tmp39 ;
  u_char *__cil_tmp40 ;
  unsigned long __cil_tmp41 ;
  unsigned long __cil_tmp42 ;
  u_char *__cil_tmp43 ;
  char const *__cil_tmp44 ;
  unsigned int __cil_tmp45 ;
  unsigned long __cil_tmp46 ;
  unsigned long __cil_tmp47 ;
  u_int __cil_tmp48 ;
  unsigned long __cil_tmp49 ;
  void *__cil_tmp50 ;
  void const *__cil_tmp51 ;
  u_int __cil_tmp52 ;

  {
# 13 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp18 = 5 * 1UL;
# 13 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp19 = (unsigned long )(data) + __cil_tmp18;
# 13 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  *((u_char *)__cil_tmp19) = (char)0;
# 15 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  cp = rrp;
# 16 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  eom = msg + msglen;
  {
# 18 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  while (1) {
    while_0_continue: ;
    {
# 18 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    dlen = nondet_short();
# 18 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    cp = cp + 2;
    }
    goto while_0_break;
  }
  while_0_break: ;
  }
  {
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  while (1) {
    while_1_continue: ;
    {
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    __cil_tmp20 = (unsigned long )eom;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    __cil_tmp21 = cp + dlen;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    __cil_tmp22 = (unsigned long )__cil_tmp21;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    if (__cil_tmp22 > __cil_tmp20) {
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
      return (-1);
    } else {

    }
    }
    goto while_1_break;
  }
  while_1_break: ;
  }
  {
# 21 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp23 = (u_char const *)msg;
# 21 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp24 = (u_char const *)eom;
# 21 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp25 = (u_char const *)cp;
# 21 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  n = dn_expand(__cil_tmp23, __cil_tmp24, __cil_tmp25, dname, namelen);
  }
# 21 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  if (n < 0) {
# 22 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    return (-1);
  } else {

  }
  {
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  cp = cp + n;
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp26 = (u_char const *)msg;
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp27 = (u_char const *)eom;
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp28 = (u_char const *)cp;
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp29 = 0 * 1UL;
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp30 = (unsigned long )(data) + __cil_tmp29;
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp31 = (u_char *)__cil_tmp30;
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp32 = (int )6UL;
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  n = dn_expand(__cil_tmp26, __cil_tmp27, __cil_tmp28, __cil_tmp31, __cil_tmp32);
  }
# 31 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  if (n < 0) {
# 32 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    return (-1);
  } else {
# 31 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    if (n >= dlen) {
# 32 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
      return (-1);
    } else {

    }
  }
  {
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  tmp = nondet_int();
  }
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  if (tmp) {
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    return (-1);
  } else {

  }
  {
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  cp = cp + n;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp33 = 0 * 1UL;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp34 = (unsigned long )(data) + __cil_tmp33;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp35 = (u_char *)__cil_tmp34;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp36 = (char const *)__cil_tmp35;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  tmp___0 = strlen(__cil_tmp36);
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp37 = 0 * 1UL;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp38 = (unsigned long )(data) + __cil_tmp37;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp39 = (u_char *)__cil_tmp38;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp40 = __cil_tmp39 + tmp___0;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  cp1 = __cil_tmp40 + 1;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp41 = 0 * 1UL;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp42 = (unsigned long )(data) + __cil_tmp41;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp43 = (u_char *)__cil_tmp42;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp44 = (char const *)__cil_tmp43;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  tmp___1 = strlen(__cil_tmp44);
  }
  {
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp45 = tmp___1 + 1U;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp46 = (unsigned long )__cil_tmp45;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp47 = 6UL - __cil_tmp46;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp48 = dlen - n;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp49 = (unsigned long )__cil_tmp48;
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  if (__cil_tmp49 <= __cil_tmp47) {

  } else {
    {
# 42 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
    __assert_fail("dlen - n <= sizeof data - (strlen((char *)data) + 1)", "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c",
                  42U, "rrextract");
    }
  }
  }
  {
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp50 = (void *)cp1;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp51 = (void const *)cp;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp52 = dlen - n;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  r_memcpy(__cil_tmp50, __cil_tmp51, __cil_tmp52);
  }
# 46 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  return (0);
}
}
# 49 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
int main(void)
{ int msglen ;
  int ret ;
  u_char *dp ;
  u_char name[3] ;
  u_char msg[6] ;
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
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp6 = 2 * 1UL;
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp7 = (unsigned long )(name) + __cil_tmp6;
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  *((u_char *)__cil_tmp7) = (char)0;
# 57 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp8 = 5 * 1UL;
# 57 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp9 = (unsigned long )(msg) + __cil_tmp8;
# 57 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  *((u_char *)__cil_tmp9) = (char)0;
# 59 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  msglen = 4;
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp10 = 0 * 1UL;
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp11 = (unsigned long )(msg) + __cil_tmp10;
# 60 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  dp = (u_char *)__cil_tmp11;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp12 = 0 * 1UL;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp13 = (unsigned long )(msg) + __cil_tmp12;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp14 = (u_char *)__cil_tmp13;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp15 = 0 * 1UL;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp16 = (unsigned long )(name) + __cil_tmp15;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  __cil_tmp17 = (u_char *)__cil_tmp16;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  ret = rrextract(__cil_tmp14, msglen, dp, __cil_tmp17, 3);
  }
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_two_expands_bad.c"
  return (0);
}
}
