# 1 "./int2.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./int2.cil.c"
# 3 "int2.c"
int multiply(int a , int b )
{

  {
# 7 "int2.c"
  return (a * b);
}
}
# 11 "int2.c"
int main(void)
{ int x ;
  int z ;
  int v ;
  int v___0 ;
  int __cil_tmp5 ;

  {
# 14 "int2.c"
  x = 3;
# 15 "int2.c"
  v = 4;
# 16 "int2.c"
  z = 2;
# 17 "int2.c"
  __cil_tmp5 = z * v;
# 17 "int2.c"
  multiply(x, __cil_tmp5);
# 18 "int2.c"
  v___0 = 0;
# 19 "int2.c"
  v___0 = multiply(v___0, x);
# 21 "int2.c"
  return (0);
}
}
