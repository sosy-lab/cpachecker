# 1 "./composite.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./composite.cil.c"
# 3 "composite.c"
struct inner {
   char x1 ;
   int x2 ;
};
# 9 "composite.c"
struct label {
   int data ;
   int data2 ;
   char test[300] ;
   int *pnt ;
   struct inner in ;
};
# 1 "composite.c"
extern void *CPAmalloc(int size ) ;
# 17 "composite.c"
void test(struct label *tst )
{ unsigned int __cil_tmp2 ;
  unsigned int __cil_tmp3 ;
  unsigned int __cil_tmp4 ;
  unsigned int __cil_tmp5 ;
  int __cil_tmp6 ;
  unsigned int __cil_tmp7 ;
  unsigned int __cil_tmp8 ;

  {
# 18 "composite.c"
  __cil_tmp2 = (unsigned int )tst;
# 18 "composite.c"
  __cil_tmp3 = __cil_tmp2 + 4;
# 18 "composite.c"
  __cil_tmp4 = (unsigned int )tst;
# 18 "composite.c"
  __cil_tmp5 = __cil_tmp4 + 4;
# 18 "composite.c"
  __cil_tmp6 = *((int *)__cil_tmp5);
# 18 "composite.c"
  *((int *)__cil_tmp3) = __cil_tmp6 + 4;
# 19 "composite.c"
  __cil_tmp7 = (unsigned int )tst;
# 19 "composite.c"
  __cil_tmp8 = __cil_tmp7 + 312;
# 19 "composite.c"
  *((char *)__cil_tmp8) = (char)22;
# 20 "composite.c"
  return;
}
}
# 23 "composite.c"
int main(void)
{ struct label name ;
  int *pnt ;
  int x ;
  struct inner *k ;
  int a ;
  struct label *stpnt ;
  void *tmp ;
  unsigned int __cil_tmp8 ;
  unsigned int __cil_tmp9 ;
  unsigned int __cil_tmp10 ;
  unsigned int __cil_tmp11 ;
  unsigned int __cil_tmp12 ;
  unsigned int __cil_tmp13 ;
  unsigned int __cil_tmp14 ;
  unsigned int __cil_tmp15 ;
  unsigned int __cil_tmp16 ;
  char __cil_tmp17 ;
  int __cil_tmp18 ;
  int __cil_tmp19 ;
  unsigned int __cil_tmp20 ;
  unsigned int __cil_tmp21 ;
  unsigned int __cil_tmp22 ;
  unsigned int __cil_tmp23 ;
  unsigned int __cil_tmp24 ;
  unsigned int __cil_tmp25 ;
  unsigned int __cil_tmp26 ;
  unsigned int __cil_tmp27 ;
  unsigned int __cil_tmp28 ;
  char __cil_tmp29 ;
  int __cil_tmp30 ;
  int __cil_tmp31 ;
  unsigned int __cil_tmp32 ;
  unsigned int __cil_tmp33 ;
  unsigned int __cil_tmp34 ;
  char __cil_tmp35 ;
  int __cil_tmp36 ;
  unsigned int __cil_tmp37 ;
  unsigned int __cil_tmp38 ;
  unsigned int __cil_tmp39 ;
  unsigned int __cil_tmp40 ;
  unsigned int __cil_tmp41 ;
  unsigned int __cil_tmp42 ;
  char __cil_tmp43 ;
  int __cil_tmp44 ;
  int __cil_tmp45 ;
  unsigned int __cil_tmp46 ;
  unsigned int __cil_tmp47 ;
  int *__cil_tmp48 ;
  int *__cil_tmp49 ;
  int *__cil_tmp50 ;
  int __cil_tmp51 ;
  struct label *__cil_tmp52 ;
  struct label *__cil_tmp53 ;
  int __cil_tmp54 ;
  struct label *__cil_tmp55 ;
  int __cil_tmp56 ;
  int __cil_tmp57 ;
  unsigned int __cil_tmp58 ;
  unsigned int __cil_tmp59 ;
  unsigned int __cil_tmp60 ;
  unsigned int __cil_tmp61 ;
  unsigned int __cil_tmp62 ;
  unsigned int __cil_tmp63 ;
  unsigned int __cil_tmp64 ;
  unsigned int __cil_tmp65 ;
  unsigned int __cil_tmp66 ;
  unsigned int __cil_tmp67 ;
  int __cil_tmp68 ;
  unsigned int __cil_tmp69 ;
  unsigned int __cil_tmp70 ;
  int __cil_tmp71 ;
  unsigned int __cil_tmp72 ;
  unsigned int __cil_tmp73 ;
  int __cil_tmp74 ;
  unsigned int __cil_tmp75 ;
  char __cil_tmp76 ;
  int __cil_tmp77 ;
  int __cil_tmp78 ;
  unsigned int __cil_tmp79 ;
  unsigned int __cil_tmp80 ;
  unsigned int __cil_tmp81 ;
  char __cil_tmp82 ;
  int __cil_tmp83 ;

  {
# 26 "composite.c"
  pnt = (int *)(& name);
# 27 "composite.c"
  pnt ++;
# 28 "composite.c"
  *pnt = 33;
# 31 "composite.c"
  __cil_tmp8 = 5 * 1U;
# 31 "composite.c"
  __cil_tmp9 = 8 + __cil_tmp8;
# 31 "composite.c"
  __cil_tmp10 = (unsigned int )(& name) + __cil_tmp9;
# 31 "composite.c"
  *((char *)__cil_tmp10) = (char)4;
# 32 "composite.c"
  __cil_tmp11 = 5 * 1U;
# 32 "composite.c"
  __cil_tmp12 = 8 + __cil_tmp11;
# 32 "composite.c"
  __cil_tmp13 = (unsigned int )(& name) + __cil_tmp12;
# 32 "composite.c"
  __cil_tmp14 = 5 * 1U;
# 32 "composite.c"
  __cil_tmp15 = 8 + __cil_tmp14;
# 32 "composite.c"
  __cil_tmp16 = (unsigned int )(& name) + __cil_tmp15;
# 32 "composite.c"
  __cil_tmp17 = *((char *)__cil_tmp16);
# 32 "composite.c"
  __cil_tmp18 = (int )__cil_tmp17;
# 32 "composite.c"
  __cil_tmp19 = __cil_tmp18 + 1;
# 32 "composite.c"
  *((char *)__cil_tmp13) = (char )__cil_tmp19;
# 33 "composite.c"
  __cil_tmp20 = 299 * 1U;
# 33 "composite.c"
  __cil_tmp21 = 8 + __cil_tmp20;
# 33 "composite.c"
  __cil_tmp22 = (unsigned int )(& name) + __cil_tmp21;
# 33 "composite.c"
  *((char *)__cil_tmp22) = (char)44;
# 34 "composite.c"
  __cil_tmp23 = 299 * 1U;
# 34 "composite.c"
  __cil_tmp24 = 8 + __cil_tmp23;
# 34 "composite.c"
  __cil_tmp25 = (unsigned int )(& name) + __cil_tmp24;
# 34 "composite.c"
  __cil_tmp26 = 299 * 1U;
# 34 "composite.c"
  __cil_tmp27 = 8 + __cil_tmp26;
# 34 "composite.c"
  __cil_tmp28 = (unsigned int )(& name) + __cil_tmp27;
# 34 "composite.c"
  __cil_tmp29 = *((char *)__cil_tmp28);
# 34 "composite.c"
  __cil_tmp30 = (int )__cil_tmp29;
# 34 "composite.c"
  __cil_tmp31 = __cil_tmp30 + 1;
# 34 "composite.c"
  *((char *)__cil_tmp25) = (char )__cil_tmp31;
  {
# 35 "composite.c"
  __cil_tmp32 = 5 * 1U;
# 35 "composite.c"
  __cil_tmp33 = 8 + __cil_tmp32;
# 35 "composite.c"
  __cil_tmp34 = (unsigned int )(& name) + __cil_tmp33;
# 35 "composite.c"
  __cil_tmp35 = *((char *)__cil_tmp34);
# 35 "composite.c"
  __cil_tmp36 = (int )__cil_tmp35;
# 35 "composite.c"
  if (__cil_tmp36 > 3) {
# 36 "composite.c"
    __cil_tmp37 = 5 * 1U;
# 36 "composite.c"
    __cil_tmp38 = 8 + __cil_tmp37;
# 36 "composite.c"
    __cil_tmp39 = (unsigned int )(& name) + __cil_tmp38;
# 36 "composite.c"
    __cil_tmp40 = 5 * 1U;
# 36 "composite.c"
    __cil_tmp41 = 8 + __cil_tmp40;
# 36 "composite.c"
    __cil_tmp42 = (unsigned int )(& name) + __cil_tmp41;
# 36 "composite.c"
    __cil_tmp43 = *((char *)__cil_tmp42);
# 36 "composite.c"
    __cil_tmp44 = (int )__cil_tmp43;
# 36 "composite.c"
    __cil_tmp45 = __cil_tmp44 + 1;
# 36 "composite.c"
    *((char *)__cil_tmp39) = (char )__cil_tmp45;
  }
  }
# 39 "composite.c"
  a = 3;
# 40 "composite.c"
  __cil_tmp46 = (unsigned int )(& name) + 308;
# 40 "composite.c"
  *((int **)__cil_tmp46) = & x;
# 41 "composite.c"
  __cil_tmp47 = (unsigned int )(& name) + 308;
# 41 "composite.c"
  __cil_tmp48 = *((int **)__cil_tmp47);
# 41 "composite.c"
  *__cil_tmp48 = 44;
# 42 "composite.c"
  __cil_tmp49 = & x;
# 42 "composite.c"
  __cil_tmp50 = & x;
# 42 "composite.c"
  __cil_tmp51 = *__cil_tmp50;
# 42 "composite.c"
  *__cil_tmp49 = __cil_tmp51 + 1;
# 45 "composite.c"
  stpnt = & name;
# 46 "composite.c"
  *((int *)stpnt) = 4;
# 47 "composite.c"
  __cil_tmp52 = & name;
# 47 "composite.c"
  __cil_tmp53 = & name;
# 47 "composite.c"
  __cil_tmp54 = *((int *)__cil_tmp53);
# 47 "composite.c"
  *((int *)__cil_tmp52) = __cil_tmp54 + 1;
# 49 "composite.c"
  tmp = CPAmalloc(318);
# 49 "composite.c"
  stpnt = (struct label *)tmp;
# 50 "composite.c"
  __cil_tmp55 = & name;
# 50 "composite.c"
  __cil_tmp56 = *((int *)__cil_tmp55);
# 50 "composite.c"
  *((int *)stpnt) = 28 + __cil_tmp56;
# 51 "composite.c"
  __cil_tmp57 = *((int *)stpnt);
# 51 "composite.c"
  *((int *)stpnt) = __cil_tmp57 + 1;
# 52 "composite.c"
  __cil_tmp58 = 5 * 1U;
# 52 "composite.c"
  __cil_tmp59 = 8 + __cil_tmp58;
# 52 "composite.c"
  __cil_tmp60 = (unsigned int )stpnt;
# 52 "composite.c"
  __cil_tmp61 = __cil_tmp60 + __cil_tmp59;
# 52 "composite.c"
  *((char *)__cil_tmp61) = (char)4;
# 53 "composite.c"
  __cil_tmp62 = (unsigned int )stpnt;
# 53 "composite.c"
  __cil_tmp63 = __cil_tmp62 + 4;
# 53 "composite.c"
  *((int *)__cil_tmp63) = 0;
# 54 "composite.c"
  test(stpnt);
# 55 "composite.c"
  __cil_tmp64 = (unsigned int )stpnt;
# 55 "composite.c"
  __cil_tmp65 = __cil_tmp64 + 4;
# 55 "composite.c"
  __cil_tmp66 = (unsigned int )stpnt;
# 55 "composite.c"
  __cil_tmp67 = __cil_tmp66 + 4;
# 55 "composite.c"
  __cil_tmp68 = *((int *)__cil_tmp67);
# 55 "composite.c"
  *((int *)__cil_tmp65) = __cil_tmp68 + 1;
# 56 "composite.c"
  test(& name);
# 57 "composite.c"
  __cil_tmp69 = (unsigned int )(& name) + 4;
# 57 "composite.c"
  __cil_tmp70 = (unsigned int )(& name) + 4;
# 57 "composite.c"
  __cil_tmp71 = *((int *)__cil_tmp70);
# 57 "composite.c"
  *((int *)__cil_tmp69) = __cil_tmp71 + 1;
# 58 "composite.c"
  __cil_tmp72 = (unsigned int )(& name) + 312;
# 58 "composite.c"
  __cil_tmp73 = (unsigned int )(& name) + 4;
# 58 "composite.c"
  __cil_tmp74 = *((int *)__cil_tmp73);
# 58 "composite.c"
  __cil_tmp75 = (unsigned int )(& name) + 312;
# 58 "composite.c"
  __cil_tmp76 = *((char *)__cil_tmp75);
# 58 "composite.c"
  __cil_tmp77 = (int )__cil_tmp76;
# 58 "composite.c"
  __cil_tmp78 = __cil_tmp77 + __cil_tmp74;
# 58 "composite.c"
  *((char *)__cil_tmp72) = (char )__cil_tmp78;
# 61 "composite.c"
  __cil_tmp79 = (unsigned int )(& name) + 312;
# 61 "composite.c"
  k = (struct inner *)__cil_tmp79;
# 62 "composite.c"
  __cil_tmp80 = (unsigned int )k;
# 62 "composite.c"
  __cil_tmp81 = __cil_tmp80 + 4;
# 62 "composite.c"
  __cil_tmp82 = *((char *)k);
# 62 "composite.c"
  __cil_tmp83 = (int )__cil_tmp82;
# 62 "composite.c"
  *((int *)__cil_tmp81) = __cil_tmp83 - 2;
# 64 "composite.c"
  return (0);
}
}
