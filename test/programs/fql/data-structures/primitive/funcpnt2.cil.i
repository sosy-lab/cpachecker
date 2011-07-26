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
# 10 "funcpnt2.c"
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
  f __cil_tmp10 ;

  {
# 13 "funcpnt2.c"
  __cil_tmp3 = 0 * 4U;
# 13 "funcpnt2.c"
  __cil_tmp4 = (unsigned int )(pnt) + __cil_tmp3;
# 13 "funcpnt2.c"
  *((f *)__cil_tmp4) = & test;
# 14 "funcpnt2.c"
  __cil_tmp5 = 0 * 4U;
# 14 "funcpnt2.c"
  __cil_tmp6 = (unsigned int )(pnt) + __cil_tmp5;
# 14 "funcpnt2.c"
  __cil_tmp7 = *((f *)__cil_tmp6);
# 14 "funcpnt2.c"
  (*__cil_tmp7)(3, 4);
# 15 "funcpnt2.c"
  __cil_tmp8 = 0 * 4U;
# 15 "funcpnt2.c"
  __cil_tmp9 = (unsigned int )(pnt) + __cil_tmp8;
# 15 "funcpnt2.c"
  p = (f *)__cil_tmp9;
# 16 "funcpnt2.c"
  __cil_tmp10 = *p;
# 16 "funcpnt2.c"
  (*__cil_tmp10)(3, 4);
# 18 "funcpnt2.c"
  return (0);
}
}
