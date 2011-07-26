# 1 "./dint2.cil.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./dint2.cil.cil.c"
# 3 "dint2.c"
int a ;
# 5 "dint2.c"
int main(void)
{ int b ;
  int *__cil_tmp2 ;
  char *__cil_tmp3 ;
  char *__cil_tmp4 ;

  {
# 6 "dint2.c"
  __cil_tmp2 = & a;
# 6 "dint2.c"
  *__cil_tmp2 = 300;
# 8 "dint2.c"
  b = (int )(& a);
# 9 "dint2.c"
  b ++;
# 10 "dint2.c"
  __cil_tmp3 = (char *)b;
# 10 "dint2.c"
  *__cil_tmp3 = (char)4;
# 11 "dint2.c"
  b ++;
# 12 "dint2.c"
  __cil_tmp4 = (char *)b;
# 12 "dint2.c"
  *__cil_tmp4 = (char)35;
# 14 "dint2.c"
  return (0);
}
}
