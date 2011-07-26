# 1 "./dint.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./dint.cil.c"
# 3 "dint.c"
int c ;
# 7 "dint.c"
int main(void)
{ short x ;
  int b ;
  int *test ;
  int **v ;
  int ***z ;
  int *__cil_tmp6 ;
  int **__cil_tmp7 ;
  int ***__cil_tmp8 ;
  int ***__cil_tmp9 ;
  int **__cil_tmp10 ;
  int *__cil_tmp11 ;
  int **__cil_tmp12 ;
  int *__cil_tmp13 ;
  int **__cil_tmp14 ;
  int *__cil_tmp15 ;
  int __cil_tmp16 ;

  {
# 9 "dint.c"
  c = -503;
# 10 "dint.c"
  c = -871;
# 11 "dint.c"
  c = 200 + c;
# 13 "dint.c"
  x = (short )c;
# 15 "dint.c"
  __cil_tmp6 = & b;
# 15 "dint.c"
  *__cil_tmp6 = c;
# 18 "dint.c"
  __cil_tmp7 = & test;
# 18 "dint.c"
  *__cil_tmp7 = & b;
# 22 "dint.c"
  __cil_tmp8 = & v;
# 22 "dint.c"
  *__cil_tmp8 = & test;
# 23 "dint.c"
  __cil_tmp9 = & v;
# 23 "dint.c"
  __cil_tmp10 = *__cil_tmp9;
# 23 "dint.c"
  __cil_tmp11 = *__cil_tmp10;
# 23 "dint.c"
  *__cil_tmp11 = (int )4U;
# 24 "dint.c"
  z = & v;
# 25 "dint.c"
  __cil_tmp12 = *z;
# 25 "dint.c"
  __cil_tmp13 = *__cil_tmp12;
# 25 "dint.c"
  __cil_tmp14 = *z;
# 25 "dint.c"
  __cil_tmp15 = *__cil_tmp14;
# 25 "dint.c"
  __cil_tmp16 = *__cil_tmp15;
# 25 "dint.c"
  *__cil_tmp13 = __cil_tmp16 + 10;
# 26 "dint.c"
  return (0);
}
}
