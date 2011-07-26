# 1 "./dfunc.cil.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./dfunc.cil.cil.c"
# 1 "dfunc.c"
int test(int *a )
{ int tmp ;
  int __cil_tmp3 ;
  int *__cil_tmp4 ;

  {
# 2 "dfunc.c"
  __cil_tmp3 = *a;
# 2 "dfunc.c"
  *a = __cil_tmp3 + 2;
# 3 "dfunc.c"
  __cil_tmp4 = (int *)3;
# 3 "dfunc.c"
  tmp = test(__cil_tmp4);
# 3 "dfunc.c"
  return (tmp);
}
}
# 6 "dfunc.c"
int test2(int a )
{

  {
# 7 "dfunc.c"
  a += 3;
# 9 "dfunc.c"
  return (0);
}
}
# 12 "dfunc.c"
int main(void)
{ int b ;
  int *a ;
  int *__cil_tmp3 ;
  int *__cil_tmp4 ;
  int *__cil_tmp5 ;
  int __cil_tmp6 ;
  int *__cil_tmp7 ;
  int __cil_tmp8 ;
  int *__cil_tmp9 ;
  int *__cil_tmp10 ;
  int __cil_tmp11 ;

  {
# 13 "dfunc.c"
  __cil_tmp3 = & b;
# 13 "dfunc.c"
  *__cil_tmp3 = 66;
# 15 "dfunc.c"
  a = & b;
# 16 "dfunc.c"
  test(a);
# 18 "dfunc.c"
  __cil_tmp4 = & b;
# 18 "dfunc.c"
  __cil_tmp5 = & b;
# 18 "dfunc.c"
  __cil_tmp6 = *__cil_tmp5;
# 18 "dfunc.c"
  *__cil_tmp4 = __cil_tmp6 + 5;
# 20 "dfunc.c"
  __cil_tmp7 = & b;
# 20 "dfunc.c"
  __cil_tmp8 = *__cil_tmp7;
# 20 "dfunc.c"
  test2(__cil_tmp8);
# 22 "dfunc.c"
  __cil_tmp9 = & b;
# 22 "dfunc.c"
  __cil_tmp10 = & b;
# 22 "dfunc.c"
  __cil_tmp11 = *__cil_tmp10;
# 22 "dfunc.c"
  *__cil_tmp9 = __cil_tmp11 + 4;
# 25 "dfunc.c"
  return (0);
}
}
