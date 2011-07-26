# 1 "./dmalloc.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./dmalloc.cil.c"
# 6 "dmalloc.c"
extern int ( CPAmalloc)() ;
# 4 "dmalloc.c"
int main(void)
{ int *d ;
  int tmp ;

  {
# 6 "dmalloc.c"
  tmp = CPAmalloc(4);
# 6 "dmalloc.c"
  d = (int *)tmp;
# 12 "dmalloc.c"
  return (0);
}
}
