# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_simp_bad.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_simp_bad.cil.c"
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
# 18 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
extern int ( nondet_short)() ;
# 6 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
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
  unsigned long __cil_tmp26 ;
  unsigned long __cil_tmp27 ;
  u_char *__cil_tmp28 ;
  int __cil_tmp29 ;
  unsigned long __cil_tmp30 ;
  unsigned long __cil_tmp31 ;
  u_char *__cil_tmp32 ;
  char const *__cil_tmp33 ;
  unsigned long __cil_tmp34 ;
  unsigned long __cil_tmp35 ;
  u_char *__cil_tmp36 ;
  u_char *__cil_tmp37 ;
  unsigned long __cil_tmp38 ;
  unsigned long __cil_tmp39 ;
  u_char *__cil_tmp40 ;
  char const *__cil_tmp41 ;
  unsigned int __cil_tmp42 ;
  unsigned long __cil_tmp43 ;
  unsigned long __cil_tmp44 ;
  u_int __cil_tmp45 ;
  unsigned long __cil_tmp46 ;
  void *__cil_tmp47 ;
  void const *__cil_tmp48 ;
  u_int __cil_tmp49 ;

  {
# 13 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp18 = 5 * 1UL;
# 13 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp19 = (unsigned long )(data) + __cil_tmp18;
# 13 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  *((u_char *)__cil_tmp19) = (char)0;
# 15 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  cp = rrp;
# 16 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  eom = msg + msglen;
  {
# 18 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  while (1) {
    while_0_continue: ;
    {
# 18 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    dlen = nondet_short();
# 18 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    cp = cp + 2;
    }
    goto while_0_break;
  }
  while_0_break: ;
  }
  {
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  while (1) {
    while_1_continue: ;
    {
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    __cil_tmp20 = (unsigned long )eom;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    __cil_tmp21 = cp + dlen;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    __cil_tmp22 = (unsigned long )__cil_tmp21;
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    if (__cil_tmp22 > __cil_tmp20) {
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
      return (-1);
    } else {

    }
    }
    goto while_1_break;
  }
  while_1_break: ;
  }
  {
# 23 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp23 = (u_char const *)msg;
# 23 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp24 = (u_char const *)eom;
# 23 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp25 = (u_char const *)cp;
# 23 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp26 = 0 * 1UL;
# 23 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp27 = (unsigned long )(data) + __cil_tmp26;
# 23 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp28 = (u_char *)__cil_tmp27;
# 23 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp29 = (int )6UL;
# 23 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  n = dn_expand(__cil_tmp23, __cil_tmp24, __cil_tmp25, __cil_tmp28, __cil_tmp29);
  }
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  if (n < 0) {
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    return (-1);
  } else {
# 25 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    if (n >= dlen) {
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
      return (-1);
    } else {

    }
  }
  {
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  tmp = nondet_int();
  }
# 29 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  if (tmp) {
# 30 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    return (-1);
  } else {

  }
  {
# 32 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  cp = cp + n;
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp30 = 0 * 1UL;
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp31 = (unsigned long )(data) + __cil_tmp30;
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp32 = (u_char *)__cil_tmp31;
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp33 = (char const *)__cil_tmp32;
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  tmp___0 = strlen(__cil_tmp33);
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp34 = 0 * 1UL;
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp35 = (unsigned long )(data) + __cil_tmp34;
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp36 = (u_char *)__cil_tmp35;
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp37 = __cil_tmp36 + tmp___0;
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  cp1 = __cil_tmp37 + 1;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp38 = 0 * 1UL;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp39 = (unsigned long )(data) + __cil_tmp38;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp40 = (u_char *)__cil_tmp39;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp41 = (char const *)__cil_tmp40;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  tmp___1 = strlen(__cil_tmp41);
  }
  {
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp42 = tmp___1 + 1U;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp43 = (unsigned long )__cil_tmp42;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp44 = 6UL - __cil_tmp43;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp45 = dlen - n;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp46 = (unsigned long )__cil_tmp45;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  if (__cil_tmp46 <= __cil_tmp44) {

  } else {
    {
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
    __assert_fail("dlen - n <= sizeof data - (strlen((char *)data) + 1)", "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c",
                  36U, "rrextract");
    }
  }
  }
  {
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp47 = (void *)cp1;
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp48 = (void const *)cp;
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp49 = dlen - n;
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  r_memcpy(__cil_tmp47, __cil_tmp48, __cil_tmp49);
  }
# 40 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  return (0);
}
}
# 43 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
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
# 50 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp6 = 2 * 1UL;
# 50 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp7 = (unsigned long )(name) + __cil_tmp6;
# 50 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  *((u_char *)__cil_tmp7) = (char)0;
# 51 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp8 = 5 * 1UL;
# 51 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp9 = (unsigned long )(msg) + __cil_tmp8;
# 51 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  *((u_char *)__cil_tmp9) = (char)0;
# 53 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  msglen = 4;
# 54 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp10 = 0 * 1UL;
# 54 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp11 = (unsigned long )(msg) + __cil_tmp10;
# 54 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  dp = (u_char *)__cil_tmp11;
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp12 = 0 * 1UL;
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp13 = (unsigned long )(msg) + __cil_tmp12;
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp14 = (u_char *)__cil_tmp13;
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp15 = 0 * 1UL;
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp16 = (unsigned long )(name) + __cil_tmp15;
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  __cil_tmp17 = (u_char *)__cil_tmp16;
# 56 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  ret = rrextract(__cil_tmp14, msglen, dp, __cil_tmp17, 3);
  }
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_bad.c"
  return (0);
}
}
