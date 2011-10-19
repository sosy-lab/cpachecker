# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_simp_ok.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "../VeriSec/bindCA-1999-14rrextract-nxt_simp_ok.cil.c"
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
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
extern int ( nondet_short)() ;
# 6 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
static int rrextract(u_char *msg , int msglen , u_char *rrp , u_char *dname , int namelen )
{ u_char *eom ;
  u_char *cp ;
  u_char *cp1 ;
  u_int dlen ;
  int n ;
  int n1 ;
  int n2 ;
  u_char data[6] ;
  int tmp ;
  unsigned int tmp___0 ;
  unsigned int tmp___1 ;
  unsigned long __cil_tmp20 ;
  unsigned long __cil_tmp21 ;
  unsigned long __cil_tmp22 ;
  u_char *__cil_tmp23 ;
  unsigned long __cil_tmp24 ;
  u_char const *__cil_tmp25 ;
  u_char const *__cil_tmp26 ;
  u_char const *__cil_tmp27 ;
  unsigned long __cil_tmp28 ;
  unsigned long __cil_tmp29 ;
  u_char *__cil_tmp30 ;
  int __cil_tmp31 ;
  unsigned long __cil_tmp32 ;
  unsigned long __cil_tmp33 ;
  u_char *__cil_tmp34 ;
  char const *__cil_tmp35 ;
  unsigned int __cil_tmp36 ;
  unsigned long __cil_tmp37 ;
  unsigned long __cil_tmp38 ;
  u_char *__cil_tmp39 ;
  unsigned long __cil_tmp40 ;
  unsigned long __cil_tmp41 ;
  unsigned long __cil_tmp42 ;
  unsigned long __cil_tmp43 ;
  unsigned long __cil_tmp44 ;
  u_char *__cil_tmp45 ;
  char const *__cil_tmp46 ;
  unsigned int __cil_tmp47 ;
  unsigned long __cil_tmp48 ;
  unsigned long __cil_tmp49 ;
  u_int __cil_tmp50 ;
  unsigned long __cil_tmp51 ;
  void *__cil_tmp52 ;
  void const *__cil_tmp53 ;

  {
# 14 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp20 = 5 * 1UL;
# 14 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp21 = (unsigned long )(data) + __cil_tmp20;
# 14 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  *((u_char *)__cil_tmp21) = (char)0;
# 16 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  cp = rrp;
# 17 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  eom = msg + msglen;
  {
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  while (1) {
    while_0_continue: ;
    {
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    dlen = nondet_short();
# 19 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    cp = cp + 2;
    }
    goto while_0_break;
  }
  while_0_break: ;
  }
  {
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  while (1) {
    while_1_continue: ;
    {
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    __cil_tmp22 = (unsigned long )eom;
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    __cil_tmp23 = cp + dlen;
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    __cil_tmp24 = (unsigned long )__cil_tmp23;
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    if (__cil_tmp24 > __cil_tmp22) {
# 20 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
      return (-1);
    } else {

    }
    }
    goto while_1_break;
  }
  while_1_break: ;
  }
  {
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp25 = (u_char const *)msg;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp26 = (u_char const *)eom;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp27 = (u_char const *)cp;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp28 = 0 * 1UL;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp29 = (unsigned long )(data) + __cil_tmp28;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp30 = (u_char *)__cil_tmp29;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp31 = (int )6UL;
# 24 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  n = dn_expand(__cil_tmp25, __cil_tmp26, __cil_tmp27, __cil_tmp30, __cil_tmp31);
  }
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  if (n < 0) {
# 27 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    return (-1);
  } else {
# 26 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    if (n >= dlen) {
# 27 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
      return (-1);
    } else {

    }
  }
  {
# 30 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  tmp = nondet_int();
  }
# 30 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  if (tmp) {
# 31 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    return (-1);
  } else {

  }
  {
# 33 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  cp = cp + n;
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp32 = 0 * 1UL;
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp33 = (unsigned long )(data) + __cil_tmp32;
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp34 = (u_char *)__cil_tmp33;
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp35 = (char const *)__cil_tmp34;
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  tmp___0 = strlen(__cil_tmp35);
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp36 = tmp___0 + 1U;
# 35 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  n1 = (int )__cil_tmp36;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp37 = 0 * 1UL;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp38 = (unsigned long )(data) + __cil_tmp37;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp39 = (u_char *)__cil_tmp38;
# 36 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  cp1 = __cil_tmp39 + n1;
# 38 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  n2 = dlen - n;
  }
  {
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp40 = (unsigned long )n1;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp41 = 6UL - __cil_tmp40;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp42 = (unsigned long )n2;
# 39 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  if (__cil_tmp42 > __cil_tmp41) {
# 40 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    return (-1);
  } else {

  }
  }
  {
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp43 = 0 * 1UL;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp44 = (unsigned long )(data) + __cil_tmp43;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp45 = (u_char *)__cil_tmp44;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp46 = (char const *)__cil_tmp45;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  tmp___1 = strlen(__cil_tmp46);
  }
  {
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp47 = tmp___1 + 1U;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp48 = (unsigned long )__cil_tmp47;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp49 = 6UL - __cil_tmp48;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp50 = dlen - n;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp51 = (unsigned long )__cil_tmp50;
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  if (__cil_tmp51 <= __cil_tmp49) {

  } else {
    {
# 44 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
    __assert_fail("dlen - n <= sizeof data - (strlen((char *)data) + 1)", "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c",
                  44U, "rrextract");
    }
  }
  }
  {
# 46 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp52 = (void *)cp1;
# 46 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp53 = (void const *)cp;
# 46 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  r_memcpy(__cil_tmp52, __cil_tmp53, n2);
  }
# 48 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  return (0);
}
}
# 51 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
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
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp6 = 2 * 1UL;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp7 = (unsigned long )(name) + __cil_tmp6;
# 58 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  *((u_char *)__cil_tmp7) = (char)0;
# 59 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp8 = 5 * 1UL;
# 59 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp9 = (unsigned long )(msg) + __cil_tmp8;
# 59 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  *((u_char *)__cil_tmp9) = (char)0;
# 61 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  msglen = 4;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp10 = 0 * 1UL;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp11 = (unsigned long )(msg) + __cil_tmp10;
# 62 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  dp = (u_char *)__cil_tmp11;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp12 = 0 * 1UL;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp13 = (unsigned long )(msg) + __cil_tmp12;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp14 = (u_char *)__cil_tmp13;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp15 = 0 * 1UL;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp16 = (unsigned long )(name) + __cil_tmp15;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  __cil_tmp17 = (u_char *)__cil_tmp16;
# 64 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  ret = rrextract(__cil_tmp14, msglen, dp, __cil_tmp17, 3);
  }
# 66 "../versisec/bind/progs1/bindCA-1999-14rrextract-nxt_simp_ok.c"
  return (0);
}
}
