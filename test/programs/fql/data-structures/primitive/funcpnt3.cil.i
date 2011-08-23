# 1 "./funcpnt3.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./funcpnt3.cil.c"
# 7 "funcpnt3.c"
struct test1 {
   int x ;
   void (*pnt)() ;
};
# 2 "funcpnt3.c"
void test(void)
{ int x ;

  {
# 4 "funcpnt3.c"
  x = 3;
# 5 "funcpnt3.c"
  return;
}
}
# 12 "funcpnt3.c"
int main(void)
{ struct test1 lab ;
  struct test1 *k ;
  unsigned int __cil_tmp3 ;
  unsigned int __cil_tmp4 ;
  unsigned int __cil_tmp5 ;
  unsigned int __cil_tmp6 ;
  void (*__cil_tmp7)() ;

  {
# 15 "funcpnt3.c"
  k = & lab;
# 16 "funcpnt3.c"
  __cil_tmp3 = (unsigned int )k;
# 16 "funcpnt3.c"
  __cil_tmp4 = __cil_tmp3 + 4;
# 16 "funcpnt3.c"
  *((void (**)())__cil_tmp4) = & test;
# 17 "funcpnt3.c"
  __cil_tmp5 = (unsigned int )k;
# 17 "funcpnt3.c"
  __cil_tmp6 = __cil_tmp5 + 4;
# 17 "funcpnt3.c"
  __cil_tmp7 = *((void (**)())__cil_tmp6);
# 17 "funcpnt3.c"
  (*__cil_tmp7)();
# 20 "funcpnt3.c"
  return (0);
}
}
