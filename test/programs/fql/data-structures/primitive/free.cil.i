# 1 "./free.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./free.cil.c"
# 1 "free.c"
union un {
   char x ;
   int v ;
};
# 8 "free.c"
extern int ( CPAmalloc)() ;
# 13 "free.c"
extern int ( CPAfree)() ;
# 7 "free.c"
int main(void)
{ int *d ;
  int tmp ;
  int **f ;
  union un *data ;
  int tmp___0 ;
  int **__cil_tmp6 ;
  int **__cil_tmp7 ;
  int *__cil_tmp8 ;
  int *__cil_tmp9 ;
  char __cil_tmp10 ;
  int __cil_tmp11 ;
  int __cil_tmp12 ;

  {
# 8 "free.c"
  tmp = CPAmalloc(4);
# 8 "free.c"
  __cil_tmp6 = & d;
# 8 "free.c"
  *__cil_tmp6 = (int *)tmp;
# 10 "free.c"
  __cil_tmp7 = & d;
# 10 "free.c"
  __cil_tmp8 = *__cil_tmp7;
# 10 "free.c"
  *__cil_tmp8 = 4;
# 12 "free.c"
  f = & d;
# 13 "free.c"
  __cil_tmp9 = *f;
# 13 "free.c"
  CPAfree(__cil_tmp9);
# 15 "free.c"
  tmp___0 = CPAmalloc(4);
# 15 "free.c"
  data = (union un *)tmp___0;
# 16 "free.c"
  *((int *)data) = 777777;
# 17 "free.c"
  __cil_tmp10 = *((char *)data);
# 17 "free.c"
  __cil_tmp11 = (int )__cil_tmp10;
# 17 "free.c"
  __cil_tmp12 = __cil_tmp11 + 72;
# 17 "free.c"
  *((char *)data) = (char )__cil_tmp12;
# 18 "free.c"
  return (0);
}
}
