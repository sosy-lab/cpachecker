# 1 "./point.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./point.cil.c"
# 3 "point.c"
int main(void)
{ int x ;
  short *test ;
  short *k ;
  short z ;
  int *__cil_tmp5 ;
  short __cil_tmp6 ;
  int __cil_tmp7 ;
  int __cil_tmp8 ;
  short __cil_tmp9 ;
  int __cil_tmp10 ;
  int __cil_tmp11 ;
  int *__cil_tmp12 ;
  int __cil_tmp13 ;
  int *__cil_tmp14 ;
  int __cil_tmp15 ;
  short *__cil_tmp16 ;
  unsigned int __cil_tmp17 ;
  unsigned int __cil_tmp18 ;
  int *__cil_tmp19 ;
  unsigned int __cil_tmp20 ;
  unsigned int __cil_tmp21 ;
  int *__cil_tmp22 ;
  int *__cil_tmp23 ;
  int __cil_tmp24 ;
  int *__cil_tmp25 ;
  int *__cil_tmp26 ;
  int __cil_tmp27 ;
  int *__cil_tmp28 ;
  int *__cil_tmp29 ;
  int __cil_tmp30 ;

  {
# 4 "point.c"
  __cil_tmp5 = & x;
# 4 "point.c"
  *__cil_tmp5 = 55;
# 7 "point.c"
  test = (short *)(& x);
# 8 "point.c"
  __cil_tmp6 = *test;
# 8 "point.c"
  __cil_tmp7 = (int )__cil_tmp6;
# 8 "point.c"
  __cil_tmp8 = __cil_tmp7 + 5;
# 8 "point.c"
  *test = (short )__cil_tmp8;
# 9 "point.c"
  test ++;
# 10 "point.c"
  __cil_tmp9 = *test;
# 10 "point.c"
  __cil_tmp10 = (int )__cil_tmp9;
# 10 "point.c"
  __cil_tmp11 = __cil_tmp10 + 1;
# 10 "point.c"
  *test = (short )__cil_tmp11;
# 12 "point.c"
  __cil_tmp12 = & x;
# 12 "point.c"
  __cil_tmp13 = *__cil_tmp12;
# 12 "point.c"
  z = (short )__cil_tmp13;
# 13 "point.c"
  __cil_tmp14 = & x;
# 13 "point.c"
  __cil_tmp15 = (int )z;
# 13 "point.c"
  *__cil_tmp14 = __cil_tmp15 + 3600;
# 16 "point.c"
  test = (short *)0;
  {
# 17 "point.c"
  __cil_tmp16 = (short *)0;
# 17 "point.c"
  __cil_tmp17 = (unsigned int )__cil_tmp16;
# 17 "point.c"
  __cil_tmp18 = (unsigned int )test;
# 17 "point.c"
  if (__cil_tmp18 == __cil_tmp17) {
# 18 "point.c"
    __cil_tmp19 = & x;
# 18 "point.c"
    *__cil_tmp19 = 51;
  }
  }
# 20 "point.c"
  test = (short *)(& x);
# 21 "point.c"
  k = (short *)(& x);
  {
# 22 "point.c"
  __cil_tmp20 = (unsigned int )k;
# 22 "point.c"
  __cil_tmp21 = (unsigned int )test;
# 22 "point.c"
  if (__cil_tmp21 == __cil_tmp20) {
# 23 "point.c"
    __cil_tmp22 = & x;
# 23 "point.c"
    __cil_tmp23 = & x;
# 23 "point.c"
    __cil_tmp24 = *__cil_tmp23;
# 23 "point.c"
    *__cil_tmp22 = __cil_tmp24 + 4;
  }
  }
# 26 "point.c"
  __cil_tmp25 = & x;
# 26 "point.c"
  __cil_tmp26 = & x;
# 26 "point.c"
  __cil_tmp27 = *__cil_tmp26;
# 26 "point.c"
  *__cil_tmp25 = 3 >= __cil_tmp27;
# 27 "point.c"
  __cil_tmp28 = & x;
# 27 "point.c"
  __cil_tmp29 = & x;
# 27 "point.c"
  __cil_tmp30 = *__cil_tmp29;
# 27 "point.c"
  *__cil_tmp28 = __cil_tmp30 + 1;
# 28 "point.c"
  return (0);
}
}
