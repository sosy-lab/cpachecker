# 1 "./composite.cil.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./composite.cil.cil.c"
# 3 "composite.c"
struct label {
   int data ;
   int data2 ;
   char test[300] ;
   int *pnt ;
};
# 3 "composite.c"
struct label test33 ;
# 33 "composite.c"
extern int ( CPAmalloc)() ;
# 10 "composite.c"
int main(void)
{ struct label name ;
  int *pnt ;
  int x ;
  int a ;
  struct label *stpnt ;
  void *tmp ;
  unsigned int __cil_tmp7 ;
  unsigned int __cil_tmp8 ;
  unsigned int __cil_tmp9 ;
  unsigned int __cil_tmp10 ;
  unsigned int __cil_tmp11 ;
  unsigned int __cil_tmp12 ;
  unsigned int __cil_tmp13 ;
  unsigned int __cil_tmp14 ;
  unsigned int __cil_tmp15 ;
  char __cil_tmp16 ;
  int __cil_tmp17 ;
  int __cil_tmp18 ;
  unsigned int __cil_tmp19 ;
  unsigned int __cil_tmp20 ;
  unsigned int __cil_tmp21 ;
  unsigned int __cil_tmp22 ;
  unsigned int __cil_tmp23 ;
  unsigned int __cil_tmp24 ;
  unsigned int __cil_tmp25 ;
  unsigned int __cil_tmp26 ;
  unsigned int __cil_tmp27 ;
  char __cil_tmp28 ;
  int __cil_tmp29 ;
  int __cil_tmp30 ;
  unsigned int __cil_tmp31 ;
  unsigned int __cil_tmp32 ;
  unsigned int __cil_tmp33 ;
  char __cil_tmp34 ;
  int __cil_tmp35 ;
  unsigned int __cil_tmp36 ;
  unsigned int __cil_tmp37 ;
  unsigned int __cil_tmp38 ;
  unsigned int __cil_tmp39 ;
  unsigned int __cil_tmp40 ;
  unsigned int __cil_tmp41 ;
  char __cil_tmp42 ;
  int __cil_tmp43 ;
  int __cil_tmp44 ;
  unsigned int __cil_tmp45 ;
  unsigned int __cil_tmp46 ;
  int *__cil_tmp47 ;
  int *__cil_tmp48 ;
  int *__cil_tmp49 ;
  int __cil_tmp50 ;
  struct label *__cil_tmp51 ;
  struct label *__cil_tmp52 ;
  int __cil_tmp53 ;
  struct label *__cil_tmp54 ;
  int __cil_tmp55 ;
  int __cil_tmp56 ;
  int tmp___0 ;
  unsigned int __cil_tmp58 ;
  char *__cil_tmp59 ;
  unsigned int __cil_tmp60 ;
  unsigned int __cil_tmp61 ;
  char *__cil_tmp62 ;
  char *__cil_tmp63 ;
  unsigned int __cil_tmp64 ;
  char *__cil_tmp65 ;
  unsigned int __cil_tmp66 ;
  unsigned int __cil_tmp67 ;
  char *__cil_tmp68 ;
  char *__cil_tmp69 ;
  unsigned int __cil_tmp70 ;
  char *__cil_tmp71 ;
  unsigned int __cil_tmp72 ;
  unsigned int __cil_tmp73 ;
  char *__cil_tmp74 ;
  char *__cil_tmp75 ;
  unsigned int __cil_tmp76 ;
  int **__cil_tmp77 ;
  unsigned int __cil_tmp78 ;
  int **__cil_tmp79 ;
  int *__cil_tmp80 ;
  int *__cil_tmp81 ;
  int *__cil_tmp82 ;
  int *__cil_tmp83 ;
  int *__cil_tmp84 ;
  int *__cil_tmp85 ;
  int *__cil_tmp86 ;

  {
# 13 "composite.c"
  pnt = (int *)(& name);
# 15 "composite.c"
  __cil_tmp7 = 5U;
# 15 "composite.c"
  __cil_tmp8 = 8U + __cil_tmp7;
# 15 "composite.c"
  __cil_tmp58 = (unsigned int )(& name);
# 15 "composite.c"
  __cil_tmp9 = __cil_tmp58 + __cil_tmp8;
# 15 "composite.c"
  __cil_tmp59 = (char *)__cil_tmp9;
# 15 "composite.c"
  *__cil_tmp59 = (char)4;
# 16 "composite.c"
  __cil_tmp10 = 5U;
# 16 "composite.c"
  __cil_tmp11 = 8U + __cil_tmp10;
# 16 "composite.c"
  __cil_tmp60 = (unsigned int )(& name);
# 16 "composite.c"
  __cil_tmp12 = __cil_tmp60 + __cil_tmp11;
# 16 "composite.c"
  __cil_tmp13 = 5U;
# 16 "composite.c"
  __cil_tmp14 = 8U + __cil_tmp13;
# 16 "composite.c"
  __cil_tmp61 = (unsigned int )(& name);
# 16 "composite.c"
  __cil_tmp15 = __cil_tmp61 + __cil_tmp14;
# 16 "composite.c"
  __cil_tmp62 = (char *)__cil_tmp15;
# 16 "composite.c"
  __cil_tmp16 = *__cil_tmp62;
# 16 "composite.c"
  __cil_tmp17 = (int )__cil_tmp16;
# 16 "composite.c"
  __cil_tmp18 = __cil_tmp17 + 1;
# 16 "composite.c"
  __cil_tmp63 = (char *)__cil_tmp12;
# 16 "composite.c"
  *__cil_tmp63 = (char )__cil_tmp18;
# 17 "composite.c"
  __cil_tmp19 = 299U;
# 17 "composite.c"
  __cil_tmp20 = 8U + __cil_tmp19;
# 17 "composite.c"
  __cil_tmp64 = (unsigned int )(& name);
# 17 "composite.c"
  __cil_tmp21 = __cil_tmp64 + __cil_tmp20;
# 17 "composite.c"
  __cil_tmp65 = (char *)__cil_tmp21;
# 17 "composite.c"
  *__cil_tmp65 = (char)44;
# 18 "composite.c"
  __cil_tmp22 = 299U;
# 18 "composite.c"
  __cil_tmp23 = 8U + __cil_tmp22;
# 18 "composite.c"
  __cil_tmp66 = (unsigned int )(& name);
# 18 "composite.c"
  __cil_tmp24 = __cil_tmp66 + __cil_tmp23;
# 18 "composite.c"
  __cil_tmp25 = 299U;
# 18 "composite.c"
  __cil_tmp26 = 8U + __cil_tmp25;
# 18 "composite.c"
  __cil_tmp67 = (unsigned int )(& name);
# 18 "composite.c"
  __cil_tmp27 = __cil_tmp67 + __cil_tmp26;
# 18 "composite.c"
  __cil_tmp68 = (char *)__cil_tmp27;
# 18 "composite.c"
  __cil_tmp28 = *__cil_tmp68;
# 18 "composite.c"
  __cil_tmp29 = (int )__cil_tmp28;
# 18 "composite.c"
  __cil_tmp30 = __cil_tmp29 + 1;
# 18 "composite.c"
  __cil_tmp69 = (char *)__cil_tmp24;
# 18 "composite.c"
  *__cil_tmp69 = (char )__cil_tmp30;
# 19 "composite.c"
  __cil_tmp31 = 5U;
# 19 "composite.c"
  __cil_tmp32 = 8U + __cil_tmp31;
# 19 "composite.c"
  __cil_tmp70 = (unsigned int )(& name);
# 19 "composite.c"
  __cil_tmp33 = __cil_tmp70 + __cil_tmp32;
# 19 "composite.c"
  __cil_tmp71 = (char *)__cil_tmp33;
# 19 "composite.c"
  __cil_tmp34 = *__cil_tmp71;
# 19 "composite.c"
  __cil_tmp35 = (int )__cil_tmp34;
# 19 "composite.c"
  if (__cil_tmp35 > 3) {
# 20 "composite.c"
    __cil_tmp36 = 5U;
# 20 "composite.c"
    __cil_tmp37 = 8U + __cil_tmp36;
# 20 "composite.c"
    __cil_tmp72 = (unsigned int )(& name);
# 20 "composite.c"
    __cil_tmp38 = __cil_tmp72 + __cil_tmp37;
# 20 "composite.c"
    __cil_tmp39 = 5U;
# 20 "composite.c"
    __cil_tmp40 = 8U + __cil_tmp39;
# 20 "composite.c"
    __cil_tmp73 = (unsigned int )(& name);
# 20 "composite.c"
    __cil_tmp41 = __cil_tmp73 + __cil_tmp40;
# 20 "composite.c"
    __cil_tmp74 = (char *)__cil_tmp41;
# 20 "composite.c"
    __cil_tmp42 = *__cil_tmp74;
# 20 "composite.c"
    __cil_tmp43 = (int )__cil_tmp42;
# 20 "composite.c"
    __cil_tmp44 = __cil_tmp43 + 1;
# 20 "composite.c"
    __cil_tmp75 = (char *)__cil_tmp38;
# 20 "composite.c"
    *__cil_tmp75 = (char )__cil_tmp44;
  }
# 23 "composite.c"
  a = 3;
# 24 "composite.c"
  __cil_tmp76 = (unsigned int )(& name);
# 24 "composite.c"
  __cil_tmp45 = __cil_tmp76 + 308U;
# 24 "composite.c"
  __cil_tmp77 = (int **)__cil_tmp45;
# 24 "composite.c"
  *__cil_tmp77 = & x;
# 25 "composite.c"
  __cil_tmp78 = (unsigned int )(& name);
# 25 "composite.c"
  __cil_tmp46 = __cil_tmp78 + 308U;
# 25 "composite.c"
  __cil_tmp79 = (int **)__cil_tmp46;
# 25 "composite.c"
  __cil_tmp47 = *__cil_tmp79;
# 25 "composite.c"
  *__cil_tmp47 = 44;
# 26 "composite.c"
  __cil_tmp48 = & x;
# 26 "composite.c"
  __cil_tmp49 = & x;
# 26 "composite.c"
  __cil_tmp50 = *__cil_tmp49;
# 26 "composite.c"
  *__cil_tmp48 = __cil_tmp50 + 1;
# 29 "composite.c"
  stpnt = & name;
# 30 "composite.c"
  __cil_tmp80 = (int *)stpnt;
# 30 "composite.c"
  *__cil_tmp80 = 4;
# 31 "composite.c"
  __cil_tmp51 = & name;
# 31 "composite.c"
  __cil_tmp52 = & name;
# 31 "composite.c"
  __cil_tmp81 = (int *)__cil_tmp52;
# 31 "composite.c"
  __cil_tmp53 = *__cil_tmp81;
# 31 "composite.c"
  __cil_tmp82 = (int *)__cil_tmp51;
# 31 "composite.c"
  *__cil_tmp82 = __cil_tmp53 + 1;
# 32 "composite.c"
  stpnt = (struct label *)0;
# 33 "composite.c"
  tmp___0 = CPAmalloc(312);
# 33 "composite.c"
  tmp = (void *)tmp___0;
# 33 "composite.c"
  stpnt = (struct label *)tmp;
# 34 "composite.c"
  __cil_tmp54 = & name;
# 34 "composite.c"
  __cil_tmp83 = (int *)__cil_tmp54;
# 34 "composite.c"
  __cil_tmp55 = *__cil_tmp83;
# 34 "composite.c"
  __cil_tmp84 = (int *)stpnt;
# 34 "composite.c"
  *__cil_tmp84 = 28 + __cil_tmp55;
# 35 "composite.c"
  __cil_tmp85 = (int *)stpnt;
# 35 "composite.c"
  __cil_tmp56 = *__cil_tmp85;
# 35 "composite.c"
  __cil_tmp86 = (int *)stpnt;
# 35 "composite.c"
  *__cil_tmp86 = __cil_tmp56 + 1;
# 36 "composite.c"
  return (0);
}
}
