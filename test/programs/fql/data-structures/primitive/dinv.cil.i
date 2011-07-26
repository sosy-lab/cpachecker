# 1 "./dinv.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./dinv.cil.c"
# 1 "dinv.c"
int main(void)
{ int x ;
  int k ;
  int __cil_tmp3 ;
  int __cil_tmp4 ;
  int __cil_tmp5 ;

  {
# 2 "dinv.c"
  x = 3;
# 3 "dinv.c"
  __cil_tmp3 = ! x;
# 3 "dinv.c"
  __cil_tmp4 = ~ __cil_tmp3;
# 3 "dinv.c"
  x = ~ __cil_tmp4;
# 4 "dinv.c"
  x += 3;
# 5 "dinv.c"
  k = 7;
# 6 "dinv.c"
  k &= x;
# 7 "dinv.c"
  x += k;
# 8 "dinv.c"
  x = x == x;
# 9 "dinv.c"
  x += 2;
# 10 "dinv.c"
  x = 3 == x;
# 11 "dinv.c"
  x += 4;
  {
# 12 "dinv.c"
  __cil_tmp5 = ! x;
# 12 "dinv.c"
  if (__cil_tmp5 + 55) {
# 13 "dinv.c"
    x += 2;
  } else {
# 15 "dinv.c"
    x += 3;
  }
  }
# 18 "dinv.c"
  return (0);
}
}
