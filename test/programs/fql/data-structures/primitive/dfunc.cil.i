# 1 "./dfunc.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./dfunc.cil.c"
# 1 "dfunc.c"
int *test(int *a )
{ int __cil_tmp2 ;

  {
# 2 "dfunc.c"
  __cil_tmp2 = *a;
# 2 "dfunc.c"
  *a = __cil_tmp2 + 2;
# 3 "dfunc.c"
  return (a);
}
}
# 6 "dfunc.c"
int test2(int *aber )
{ int __cil_tmp2 ;

  {
# 7 "dfunc.c"
  __cil_tmp2 = *aber;
# 7 "dfunc.c"
  *aber = __cil_tmp2 + 3;
# 9 "dfunc.c"
  return (0);
}
}
# 12 "dfunc.c"
int main(void)
{ int b ;
  int *c ;
  int *a ;
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
  __cil_tmp4 = & b;
# 13 "dfunc.c"
  *__cil_tmp4 = 66;
# 16 "dfunc.c"
  a = & b;
# 17 "dfunc.c"
  c = test(a);
# 19 "dfunc.c"
  __cil_tmp5 = & b;
# 19 "dfunc.c"
  __cil_tmp6 = *c;
# 19 "dfunc.c"
  __cil_tmp7 = & b;
# 19 "dfunc.c"
  __cil_tmp8 = *__cil_tmp7;
# 19 "dfunc.c"
  *__cil_tmp5 = __cil_tmp8 + __cil_tmp6;
# 21 "dfunc.c"
  test2(& b);
# 22 "dfunc.c"
  test2(c);
# 23 "dfunc.c"
  __cil_tmp9 = & b;
# 23 "dfunc.c"
  __cil_tmp10 = & b;
# 23 "dfunc.c"
  __cil_tmp11 = *__cil_tmp10;
# 23 "dfunc.c"
  *__cil_tmp9 = __cil_tmp11 + 4;
# 26 "dfunc.c"
  return (0);
}
}
