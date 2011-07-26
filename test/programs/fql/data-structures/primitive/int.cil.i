# 1 "int.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "int.cil.c"
# 1 "int.c"
int main(void)
{ int v0 ;
  int v1 ;
  int v2 ;
  int v3 ;

  {
# 3 "int.c"
  v0 = 3;
# 4 "int.c"
  v1 = v0 + 3;
# 5 "int.c"
  v2 = v1 * v0;
# 6 "int.c"
  v1 *= 2;
# 8 "int.c"
  v3 = v1 * (v2 + 3);
# 9 "int.c"
  v1 = 3;
# 9 "int.c"
  if (v1) {
# 10 "int.c"
    v1 += v0;
  }
# 13 "int.c"
  return (0);
}
}
