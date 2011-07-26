# 1 "./drecursive.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./drecursive.cil.c"
# 1 "drecursive.c"
int test(int a )
{ int tmp ;
  int __cil_tmp3 ;

  {
# 2 "drecursive.c"
  if (a <= 13) {
# 3 "drecursive.c"
    __cil_tmp3 = a + 2;
# 3 "drecursive.c"
    tmp = test(__cil_tmp3);
# 3 "drecursive.c"
    return (tmp);
  } else {
# 5 "drecursive.c"
    return (a);
  }
}
}
# 10 "drecursive.c"
int test2(int a )
{

  {
# 11 "drecursive.c"
  return (a);
}
}
# 16 "drecursive.c"
int main(void)
{ int x ;
  int y ;
  int z ;

  {
# 17 "drecursive.c"
  x = 4;
# 18 "drecursive.c"
  y = 5;
# 19 "drecursive.c"
  x = x < y;
# 21 "drecursive.c"
  x += 5;
# 23 "drecursive.c"
  z = 1;
# 24 "drecursive.c"
  z = test(z);
# 25 "drecursive.c"
  z ++;
# 27 "drecursive.c"
  return (0);
}
}
