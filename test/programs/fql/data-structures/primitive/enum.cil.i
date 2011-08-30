# 1 "./enum.cil.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "./enum.cil.c"
# 4 "enum.c"
enum test {
    x = 1,
    y = 2
} ;
# 9 "enum.c"
typedef enum test x1;
# 12 "enum.c"
int main(void)
{ int v ;
  enum test data ;
  enum test *pnt ;
  x1 data2 ;
  enum test *__cil_tmp5 ;
  enum test *__cil_tmp6 ;
  enum test __cil_tmp7 ;
  enum test *__cil_tmp8 ;
  enum test __cil_tmp9 ;
  unsigned int __cil_tmp10 ;
  unsigned int __cil_tmp11 ;
  unsigned int __cil_tmp12 ;
  enum test *__cil_tmp13 ;
  unsigned int __cil_tmp14 ;
  unsigned int __cil_tmp15 ;
  unsigned int __cil_tmp16 ;

  {
# 16 "enum.c"
  __cil_tmp5 = & data;
# 16 "enum.c"
  *__cil_tmp5 = (enum test )2;
# 18 "enum.c"
  __cil_tmp6 = & data;
# 18 "enum.c"
  __cil_tmp7 = *__cil_tmp6;
# 18 "enum.c"
  v = (int )__cil_tmp7;
# 19 "enum.c"
  v ++;
# 20 "enum.c"
  pnt = & data;
# 21 "enum.c"
  *pnt = (enum test )1;
# 22 "enum.c"
  __cil_tmp8 = & data;
# 22 "enum.c"
  __cil_tmp9 = *__cil_tmp8;
# 22 "enum.c"
  __cil_tmp10 = (unsigned int )__cil_tmp9;
# 22 "enum.c"
  __cil_tmp11 = (unsigned int )v;
# 22 "enum.c"
  __cil_tmp12 = __cil_tmp11 + __cil_tmp10;
# 22 "enum.c"
  v = (int )__cil_tmp12;
# 24 "enum.c"
  __cil_tmp13 = & data;
# 24 "enum.c"
  data2 = *__cil_tmp13;
# 25 "enum.c"
  __cil_tmp14 = (unsigned int )data2;
# 25 "enum.c"
  __cil_tmp15 = (unsigned int )v;
# 25 "enum.c"
  __cil_tmp16 = __cil_tmp15 + __cil_tmp14;
# 25 "enum.c"
  v = (int )__cil_tmp16;
# 26 "enum.c"
  return (0);
}
}
