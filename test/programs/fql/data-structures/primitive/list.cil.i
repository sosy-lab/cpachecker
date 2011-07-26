# 1 "./list.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./list.cil.c"
# 2 "list.c"
struct list {
   int x ;
   struct list *next ;
};
# 8 "list.c"
int main(void)
{ int x ;
  struct list item1 ;
  struct list item2 ;
  struct list item3 ;
  struct list *it ;
  unsigned int __cil_tmp6 ;
  unsigned int __cil_tmp7 ;
  unsigned int __cil_tmp8 ;
  struct list *__cil_tmp9 ;
  unsigned int __cil_tmp10 ;
  unsigned int __cil_tmp11 ;
  unsigned int __cil_tmp12 ;
  struct list *__cil_tmp13 ;
  unsigned int __cil_tmp14 ;
  unsigned int __cil_tmp15 ;
  unsigned int __cil_tmp16 ;

  {
# 10 "list.c"
  x = 0;
# 14 "list.c"
  __cil_tmp6 = (unsigned int )(& item1) + 4;
# 14 "list.c"
  *((struct list **)__cil_tmp6) = & item2;
# 15 "list.c"
  __cil_tmp7 = (unsigned int )(& item2) + 4;
# 15 "list.c"
  *((struct list **)__cil_tmp7) = & item3;
# 16 "list.c"
  __cil_tmp8 = (unsigned int )(& item3) + 4;
# 16 "list.c"
  *((struct list **)__cil_tmp8) = (struct list *)0;
# 19 "list.c"
  it = & item1;
# 20 "list.c"
  while (1) {
    {
# 20 "list.c"
    __cil_tmp9 = (struct list *)0;
# 20 "list.c"
    __cil_tmp10 = (unsigned int )__cil_tmp9;
# 20 "list.c"
    __cil_tmp11 = (unsigned int )it;
# 20 "list.c"
    __cil_tmp12 = __cil_tmp11 + 4;
# 20 "list.c"
    __cil_tmp13 = *((struct list **)__cil_tmp12);
# 20 "list.c"
    __cil_tmp14 = (unsigned int )__cil_tmp13;
# 20 "list.c"
    if (! (__cil_tmp14 != __cil_tmp10)) {
# 20 "list.c"
      break;
    }
    }
# 21 "list.c"
    *((int *)it) = x + 1;
# 22 "list.c"
    __cil_tmp15 = (unsigned int )it;
# 22 "list.c"
    __cil_tmp16 = __cil_tmp15 + 4;
# 22 "list.c"
    it = *((struct list **)__cil_tmp16);
# 23 "list.c"
    x ++;
  }
# 26 "list.c"
  return (0);
}
}
