# 1 "./funcpnt.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./funcpnt.cil.c"
# 1 "funcpnt.c"
struct test {
   int (*pnt)(int , int ) ;
   int v ;
};
# 7 "funcpnt.c"
int test(int a , int b )
{

  {
# 8 "funcpnt.c"
  return (a + b);
}
}
# 12 "funcpnt.c"
int test2(int a , int b )
{ int tmp ;
  int (*__cil_tmp4)(int a , int b ) ;
  int __cil_tmp5 ;

  {
# 13 "funcpnt.c"
  __cil_tmp4 = & test;
# 13 "funcpnt.c"
  __cil_tmp5 = a + a;
# 13 "funcpnt.c"
  tmp = (*__cil_tmp4)(a, __cil_tmp5);
# 13 "funcpnt.c"
  return (tmp);
}
}
# 16 "funcpnt.c"
int test3(int (*a)(int , int ) , int b , int c , int *d )
{ int tmp ;
  int __cil_tmp6 ;

  {
# 17 "funcpnt.c"
  tmp = (*a)(b, c);
  {
# 17 "funcpnt.c"
  __cil_tmp6 = *d;
# 17 "funcpnt.c"
  return (tmp + __cil_tmp6);
  }
}
}
# 20 "funcpnt.c"
int (*fpnt)(int , int ) ;
# 21 "funcpnt.c"
int main(void)
{ struct test s ;
  struct test *pnt ;
  int a ;
  int b ;
  int x ;
  int tmp ;
  int *__cil_tmp7 ;
  int *__cil_tmp8 ;
  int __cil_tmp9 ;
  unsigned int __cil_tmp10 ;
  unsigned int __cil_tmp11 ;
  int (*__cil_tmp12)(int , int ) ;
  int *__cil_tmp13 ;
  int __cil_tmp14 ;
  unsigned int __cil_tmp15 ;
  unsigned int __cil_tmp16 ;
  unsigned int __cil_tmp17 ;
  unsigned int __cil_tmp18 ;
  int __cil_tmp19 ;
  int (*__cil_tmp20)(int a , int b ) ;
  int *__cil_tmp21 ;
  int __cil_tmp22 ;
  unsigned int __cil_tmp23 ;
  unsigned int __cil_tmp24 ;
  unsigned int __cil_tmp25 ;
  unsigned int __cil_tmp26 ;
  unsigned int __cil_tmp27 ;
  unsigned int __cil_tmp28 ;
  int __cil_tmp29 ;
  unsigned int __cil_tmp30 ;
  unsigned int __cil_tmp31 ;
  int (*__cil_tmp32)(int a , int b ) ;

  {
# 25 "funcpnt.c"
  pnt = & s;
# 27 "funcpnt.c"
  fpnt = & test;
# 28 "funcpnt.c"
  __cil_tmp7 = & a;
# 28 "funcpnt.c"
  *__cil_tmp7 = 3;
# 29 "funcpnt.c"
  b = 4;
# 30 "funcpnt.c"
  __cil_tmp8 = & a;
# 30 "funcpnt.c"
  __cil_tmp9 = *__cil_tmp8;
# 30 "funcpnt.c"
  tmp = (*fpnt)(__cil_tmp9, b);
# 30 "funcpnt.c"
  x = tmp;
# 31 "funcpnt.c"
  *((int (**)(int , int ))pnt) = fpnt;
# 32 "funcpnt.c"
  __cil_tmp10 = (unsigned int )pnt;
# 32 "funcpnt.c"
  __cil_tmp11 = __cil_tmp10 + 4;
# 32 "funcpnt.c"
  __cil_tmp12 = *((int (**)(int , int ))pnt);
# 32 "funcpnt.c"
  __cil_tmp13 = & a;
# 32 "funcpnt.c"
  __cil_tmp14 = *__cil_tmp13;
# 32 "funcpnt.c"
  *((int *)__cil_tmp11) = (*__cil_tmp12)(__cil_tmp14, b);
# 33 "funcpnt.c"
  __cil_tmp15 = (unsigned int )pnt;
# 33 "funcpnt.c"
  __cil_tmp16 = __cil_tmp15 + 4;
# 33 "funcpnt.c"
  __cil_tmp17 = (unsigned int )pnt;
# 33 "funcpnt.c"
  __cil_tmp18 = __cil_tmp17 + 4;
# 33 "funcpnt.c"
  __cil_tmp19 = *((int *)__cil_tmp18);
# 33 "funcpnt.c"
  *((int *)__cil_tmp16) = __cil_tmp19 + 1;
# 34 "funcpnt.c"
  __cil_tmp20 = & test2;
# 34 "funcpnt.c"
  __cil_tmp21 = & a;
# 34 "funcpnt.c"
  __cil_tmp22 = *__cil_tmp21;
# 34 "funcpnt.c"
  (*__cil_tmp20)(__cil_tmp22, b);
# 35 "funcpnt.c"
  __cil_tmp23 = (unsigned int )pnt;
# 35 "funcpnt.c"
  __cil_tmp24 = __cil_tmp23 + 4;
# 35 "funcpnt.c"
  *((int *)__cil_tmp24) = test3(& test2, b, 33, & a);
# 36 "funcpnt.c"
  __cil_tmp25 = (unsigned int )pnt;
# 36 "funcpnt.c"
  __cil_tmp26 = __cil_tmp25 + 4;
# 36 "funcpnt.c"
  __cil_tmp27 = (unsigned int )pnt;
# 36 "funcpnt.c"
  __cil_tmp28 = __cil_tmp27 + 4;
# 36 "funcpnt.c"
  __cil_tmp29 = *((int *)__cil_tmp28);
# 36 "funcpnt.c"
  *((int *)__cil_tmp26) = __cil_tmp29 + 1;
# 37 "funcpnt.c"
  __cil_tmp30 = (unsigned int )pnt;
# 37 "funcpnt.c"
  __cil_tmp31 = __cil_tmp30 + 4;
# 37 "funcpnt.c"
  __cil_tmp32 = & test;
# 37 "funcpnt.c"
  *((int *)__cil_tmp31) = (*__cil_tmp32)(33, 44);
# 38 "funcpnt.c"
  return (0);
}
}
