# 1 "./funcpnt2.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./funcpnt2.cil.c"
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
# 9 "funcpnt2.c"
int test2(int a , int b )
{

  {
# 10 "funcpnt2.c"
  return (a + b);
}
}
# 14 "funcpnt2.c"
int main(void)
{ f pnt[2] ;
  f *p ;
  unsigned int __cil_tmp3 ;
  unsigned int __cil_tmp4 ;
  unsigned int __cil_tmp5 ;
  unsigned int __cil_tmp6 ;
  f __cil_tmp7 ;
  unsigned int __cil_tmp8 ;
  unsigned int __cil_tmp9 ;
  unsigned int __cil_tmp10 ;
  unsigned int __cil_tmp11 ;
  f __cil_tmp12 ;
  unsigned int __cil_tmp13 ;
  unsigned int __cil_tmp14 ;
  f __cil_tmp15 ;

  {
# 18 "funcpnt2.c"
  __cil_tmp3 = 0 * 4U;
# 18 "funcpnt2.c"
  __cil_tmp4 = (unsigned int )(pnt) + __cil_tmp3;
# 18 "funcpnt2.c"
  *((f *)__cil_tmp4) = & test;
# 19 "funcpnt2.c"
  __cil_tmp5 = 0 * 4U;
# 19 "funcpnt2.c"
  __cil_tmp6 = (unsigned int )(pnt) + __cil_tmp5;
# 19 "funcpnt2.c"
  __cil_tmp7 = *((f *)__cil_tmp6);
# 19 "funcpnt2.c"
  (*__cil_tmp7)(3, 4);
# 20 "funcpnt2.c"
  __cil_tmp8 = 1 * 4U;
# 20 "funcpnt2.c"
  __cil_tmp9 = (unsigned int )(pnt) + __cil_tmp8;
# 20 "funcpnt2.c"
  *((f *)__cil_tmp9) = & test2;
# 21 "funcpnt2.c"
  __cil_tmp10 = 1 * 4U;
# 21 "funcpnt2.c"
  __cil_tmp11 = (unsigned int )(pnt) + __cil_tmp10;
# 21 "funcpnt2.c"
  __cil_tmp12 = *((f *)__cil_tmp11);
# 21 "funcpnt2.c"
  (*__cil_tmp12)(3, 9);
# 22 "funcpnt2.c"
  __cil_tmp13 = 0 * 4U;
# 22 "funcpnt2.c"
  __cil_tmp14 = (unsigned int )(pnt) + __cil_tmp13;
# 22 "funcpnt2.c"
  p = (f *)__cil_tmp14;
# 23 "funcpnt2.c"
  __cil_tmp15 = *p;
# 23 "funcpnt2.c"
  (*__cil_tmp15)(3, 4);
# 26 "funcpnt2.c"
  return (0);
}
}
