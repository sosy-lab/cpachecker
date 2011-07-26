# 1 "./typedef.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./typedef.cil.c"
# 1 "typedef.c"
typedef int *PNT;
# 2 "typedef.c"
typedef int ARR[5];
# 3 "typedef.c"
typedef PNT ARR2[5];
# 4 "typedef.c"
typedef ARR ARR3[3][10];
# 6 "typedef.c"
struct stest {
   int x ;
   int y ;
};
# 12 "typedef.c"
typedef struct stest var[5];
# 13 "typedef.c"
typedef var field[5];
# 6 "typedef.c"
struct stest asdfsd ;
# 14 "typedef.c"
int main(void)
{ int x ;
  PNT a ;
  ARR k ;
  ARR2 data ;
  ARR3 overkill ;
  var sdata ;
  field test ;
  int *__cil_tmp8 ;
  int *__cil_tmp9 ;
  int __cil_tmp10 ;
  unsigned int __cil_tmp11 ;
  unsigned int __cil_tmp12 ;
  int *__cil_tmp13 ;
  int __cil_tmp14 ;
  unsigned int __cil_tmp15 ;
  unsigned int __cil_tmp16 ;
  unsigned int __cil_tmp17 ;
  unsigned int __cil_tmp18 ;
  PNT __cil_tmp19 ;
  int *__cil_tmp20 ;
  int __cil_tmp21 ;
  unsigned int __cil_tmp22 ;
  unsigned int __cil_tmp23 ;
  unsigned int __cil_tmp24 ;
  unsigned int __cil_tmp25 ;
  unsigned int __cil_tmp26 ;
  unsigned int __cil_tmp27 ;
  unsigned int __cil_tmp28 ;
  unsigned int __cil_tmp29 ;
  unsigned int __cil_tmp30 ;
  unsigned int __cil_tmp31 ;
  unsigned int __cil_tmp32 ;
  unsigned int __cil_tmp33 ;
  unsigned int __cil_tmp34 ;
  unsigned int __cil_tmp35 ;
  unsigned int __cil_tmp36 ;
  unsigned int __cil_tmp37 ;
  unsigned int __cil_tmp38 ;
  unsigned int __cil_tmp39 ;

  {
# 20 "typedef.c"
  a = & x;
# 21 "typedef.c"
  *a = 7;
# 22 "typedef.c"
  __cil_tmp8 = & x;
# 22 "typedef.c"
  __cil_tmp9 = & x;
# 22 "typedef.c"
  __cil_tmp10 = *__cil_tmp9;
# 22 "typedef.c"
  *__cil_tmp8 = __cil_tmp10 * 7;
# 23 "typedef.c"
  __cil_tmp11 = 1 * 4U;
# 23 "typedef.c"
  __cil_tmp12 = (unsigned int )(k) + __cil_tmp11;
# 23 "typedef.c"
  __cil_tmp13 = & x;
# 23 "typedef.c"
  __cil_tmp14 = *__cil_tmp13;
# 23 "typedef.c"
  *((int *)__cil_tmp12) = __cil_tmp14 + 4;
# 25 "typedef.c"
  __cil_tmp15 = 4 * 4U;
# 25 "typedef.c"
  __cil_tmp16 = (unsigned int )(data) + __cil_tmp15;
# 25 "typedef.c"
  *((PNT *)__cil_tmp16) = a;
# 26 "typedef.c"
  __cil_tmp17 = 4 * 4U;
# 26 "typedef.c"
  __cil_tmp18 = (unsigned int )(data) + __cil_tmp17;
# 26 "typedef.c"
  __cil_tmp19 = *((PNT *)__cil_tmp18);
# 26 "typedef.c"
  __cil_tmp20 = & x;
# 26 "typedef.c"
  __cil_tmp21 = *__cil_tmp20;
# 26 "typedef.c"
  *__cil_tmp19 = __cil_tmp21 + 7;
# 28 "typedef.c"
  __cil_tmp22 = 4 * 4U;
# 28 "typedef.c"
  __cil_tmp23 = 9 * 20U;
# 28 "typedef.c"
  __cil_tmp24 = __cil_tmp23 + __cil_tmp22;
# 28 "typedef.c"
  __cil_tmp25 = 2 * 200U;
# 28 "typedef.c"
  __cil_tmp26 = __cil_tmp25 + __cil_tmp24;
# 28 "typedef.c"
  __cil_tmp27 = (unsigned int )(overkill) + __cil_tmp26;
# 28 "typedef.c"
  *((int *)__cil_tmp27) = 55;
# 29 "typedef.c"
  __cil_tmp28 = 3 * 4U;
# 29 "typedef.c"
  __cil_tmp29 = 9 * 20U;
# 29 "typedef.c"
  __cil_tmp30 = __cil_tmp29 + __cil_tmp28;
# 29 "typedef.c"
  __cil_tmp31 = 2 * 200U;
# 29 "typedef.c"
  __cil_tmp32 = __cil_tmp31 + __cil_tmp30;
# 29 "typedef.c"
  __cil_tmp33 = (unsigned int )(overkill) + __cil_tmp32;
# 29 "typedef.c"
  *((int *)__cil_tmp33) = 3;
# 31 "typedef.c"
  __cil_tmp34 = 0 * 8U;
# 31 "typedef.c"
  __cil_tmp35 = (unsigned int )(sdata) + __cil_tmp34;
# 31 "typedef.c"
  *((int *)__cil_tmp35) = 44;
# 33 "typedef.c"
  __cil_tmp36 = 0 * 8U;
# 33 "typedef.c"
  __cil_tmp37 = 0 * 40U;
# 33 "typedef.c"
  __cil_tmp38 = __cil_tmp37 + __cil_tmp36;
# 33 "typedef.c"
  __cil_tmp39 = (unsigned int )(test) + __cil_tmp38;
# 33 "typedef.c"
  *((int *)__cil_tmp39) = 44;
# 34 "typedef.c"
  return (0);
}
}
