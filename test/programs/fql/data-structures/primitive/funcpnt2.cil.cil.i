# 1 "./funcpnt2.cil.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./funcpnt2.cil.cil.c"
# 3 "funcpnt2.c"
typedef int (*f)(int , int );
# 5 "funcpnt2.c"
int test(int a , int b )
{

  {
# 6 "funcpnt2.c"
  return (a - b);
}
}
# 10 "funcpnt2.c"
int main(void)
{ f pnt[2] ;
  f *p ;
  unsigned int __cil_tmp3 ;
  unsigned int __cil_tmp4 ;
  unsigned int __cil_tmp5 ;
  unsigned int __cil_tmp6 ;
  int (*__cil_tmp7)(int , int ) ;
  unsigned int __cil_tmp8 ;
  unsigned int __cil_tmp9 ;
  int (*__cil_tmp10)(int , int ) ;
  int (*__cil_tmp11)(int , int ) ;
  unsigned int __cil_tmp12 ;
  unsigned int __cil_tmp13 ;
  f *__cil_tmp14 ;
  unsigned int __cil_tmp15 ;
  f *__cil_tmp16 ;
  unsigned int __cil_tmp17 ;
  unsigned int __cil_tmp18 ;
  f *__cil_tmp19 ;
  unsigned int __cil_tmp20 ;
  f *__cil_tmp21 ;
  unsigned int __cil_tmp22 ;
  unsigned int __cil_tmp23 ;
  f *__cil_tmp24 ;
  unsigned int __cil_tmp25 ;
  f *__cil_tmp26 ;

  {
# 13 "funcpnt2.c"
  __cil_tmp3 = 0U;
# 13 "funcpnt2.c"
  __cil_tmp12 = 0 * 4U;
# 13 "funcpnt2.c"
  __cil_tmp13 = (unsigned int )(pnt) + __cil_tmp12;
# 13 "funcpnt2.c"
  __cil_tmp14 = (f *)__cil_tmp13;
# 13 "funcpnt2.c"
  __cil_tmp15 = (unsigned int )__cil_tmp14;
# 13 "funcpnt2.c"
  __cil_tmp4 = __cil_tmp15 + __cil_tmp3;
# 13 "funcpnt2.c"
  __cil_tmp16 = (f *)__cil_tmp4;
# 13 "funcpnt2.c"
  *__cil_tmp16 = & test;
# 14 "funcpnt2.c"
  __cil_tmp5 = 0U;
# 14 "funcpnt2.c"
  __cil_tmp17 = 0 * 4U;
# 14 "funcpnt2.c"
  __cil_tmp18 = (unsigned int )(pnt) + __cil_tmp17;
# 14 "funcpnt2.c"
  __cil_tmp19 = (f *)__cil_tmp18;
# 14 "funcpnt2.c"
  __cil_tmp20 = (unsigned int )__cil_tmp19;
# 14 "funcpnt2.c"
  __cil_tmp6 = __cil_tmp20 + __cil_tmp5;
# 14 "funcpnt2.c"
  __cil_tmp21 = (f *)__cil_tmp6;
# 14 "funcpnt2.c"
  __cil_tmp7 = *__cil_tmp21;
# 14 "funcpnt2.c"
  (*__cil_tmp7)(3, 4);
# 15 "funcpnt2.c"
  __cil_tmp8 = 0U;
# 15 "funcpnt2.c"
  __cil_tmp22 = 0 * 4U;
# 15 "funcpnt2.c"
  __cil_tmp23 = (unsigned int )(pnt) + __cil_tmp22;
# 15 "funcpnt2.c"
  __cil_tmp24 = (f *)__cil_tmp23;
# 15 "funcpnt2.c"
  __cil_tmp25 = (unsigned int )__cil_tmp24;
# 15 "funcpnt2.c"
  __cil_tmp9 = __cil_tmp25 + __cil_tmp8;
# 15 "funcpnt2.c"
  __cil_tmp26 = (f *)__cil_tmp9;
# 15 "funcpnt2.c"
  __cil_tmp10 = *__cil_tmp26;
# 15 "funcpnt2.c"
  p = (f *)__cil_tmp10;
# 16 "funcpnt2.c"
  __cil_tmp11 = *p;
# 16 "funcpnt2.c"
  (*__cil_tmp11)(3, 4);
# 18 "funcpnt2.c"
  return (0);
}
}
