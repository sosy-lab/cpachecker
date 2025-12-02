// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;
typedef int wchar_t;

typedef struct
  {
    int quot;
    int rem;
  } div_t;
typedef struct
  {
    long int quot;
    long int rem;
  } ldiv_t;
__extension__ typedef struct
  {
    long long int quot;
    long long int rem;
  } lldiv_t;
extern size_t __ctype_get_mb_cur_max (void) __attribute__ ((__nothrow__ , __leaf__)) ;
extern double atof (const char *__nptr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) ;
extern int atoi (const char *__nptr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) ;
extern long int atol (const char *__nptr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) ;
__extension__ extern long long int atoll (const char *__nptr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) ;
extern double strtod (const char *__restrict __nptr,
        char **__restrict __endptr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern float strtof (const char *__restrict __nptr,
       char **__restrict __endptr) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern long double strtold (const char *__restrict __nptr,
       char **__restrict __endptr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern long int strtol (const char *__restrict __nptr,
   char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern unsigned long int strtoul (const char *__restrict __nptr,
      char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
__extension__
extern long long int strtoll (const char *__restrict __nptr,
         char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
__extension__
extern unsigned long long int strtoull (const char *__restrict __nptr,
     char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int rand (void) __attribute__ ((__nothrow__ , __leaf__));
extern void srand (unsigned int __seed) __attribute__ ((__nothrow__ , __leaf__));
extern void *malloc (size_t __size) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__))
     __attribute__ ((__alloc_size__ (1))) ;
extern void *calloc (size_t __nmemb, size_t __size)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__)) __attribute__ ((__alloc_size__ (1, 2))) ;
extern void *realloc (void *__ptr, size_t __size)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__warn_unused_result__)) __attribute__ ((__alloc_size__ (2)));
extern void free (void *__ptr) __attribute__ ((__nothrow__ , __leaf__));
extern void *aligned_alloc (size_t __alignment, size_t __size)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__)) __attribute__ ((__alloc_align__ (1)))
     __attribute__ ((__alloc_size__ (2))) ;
extern void abort (void) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern int atexit (void (*__func) (void)) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int at_quick_exit (void (*__func) (void)) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern void exit (int __status) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void quick_exit (int __status) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void _Exit (int __status) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern char *getenv (const char *__name) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1))) ;
extern int system (const char *__command) ;
typedef int (*__compar_fn_t) (const void *, const void *);
extern void *bsearch (const void *__key, const void *__base,
        size_t __nmemb, size_t __size, __compar_fn_t __compar)
     __attribute__ ((__nonnull__ (1, 2, 5))) ;
extern void qsort (void *__base, size_t __nmemb, size_t __size,
     __compar_fn_t __compar) __attribute__ ((__nonnull__ (1, 4)));
extern int abs (int __x) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__const__)) ;
extern long int labs (long int __x) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__const__)) ;
__extension__ extern long long int llabs (long long int __x)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__const__)) ;
extern div_t div (int __numer, int __denom)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__const__)) ;
extern ldiv_t ldiv (long int __numer, long int __denom)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__const__)) ;
__extension__ extern lldiv_t lldiv (long long int __numer,
        long long int __denom)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__const__)) ;
extern int mblen (const char *__s, size_t __n) __attribute__ ((__nothrow__ , __leaf__));
extern int mbtowc (wchar_t *__restrict __pwc,
     const char *__restrict __s, size_t __n) __attribute__ ((__nothrow__ , __leaf__));
extern int wctomb (char *__s, wchar_t __wchar) __attribute__ ((__nothrow__ , __leaf__));
extern size_t mbstowcs (wchar_t *__restrict __pwcs,
   const char *__restrict __s, size_t __n) __attribute__ ((__nothrow__ , __leaf__))
    __attribute__ ((__access__ (__read_only__, 2)));
extern size_t wcstombs (char *__restrict __s,
   const wchar_t *__restrict __pwcs, size_t __n)
     __attribute__ ((__nothrow__ , __leaf__))
  __attribute__ ((__access__ (__write_only__, 1, 3)))
  __attribute__ ((__access__ (__read_only__, 2)));


extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert_perror_fail (int __errnum, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

typedef int (*FuncDefFoo)(int, int);
typedef int (*FuncDefFooInFun)(FuncDefFoo, int, int);
struct fooStruct {
  int (*function)(int, int);
};
struct barStruct {
  struct fooStruct *bar;
};
struct localBarStruct {
  struct fooStruct bar;
};
struct funcDefFooStruct {
  FuncDefFoo localFoo;
  FuncDefFoo * funcDefFooPtrRef;
  struct barStruct barStructRef;
  struct localBarStruct localBarStructRef;
  struct barStruct * barStructPtrRef;
  struct localBarStruct * localBarStructPtrRef;
} combiStruct;
typedef struct funcDefFooStruct renamedFuncDefFooStruct;
int foo(int a, int b) {
  return a + b;
}
int glob = 11111;
int grob = 77777;
int bar(int (*functionPtr)(int, int)) {
  return (*functionPtr)(++glob, ++grob);
}
int barBar(int (*functionPtr)(int, int), int aa, int be) {
  return (*functionPtr)(aa, be);
}
struct barBarStruct {
    int (*(*barBarPtr))(int (*)(int, int), int, int);
    int (*(*nestedBarBarArray[2]))(int (*)(int, int), int, int);
} barBarBars;
FuncDefFoo functionFunction() {
    FuncDefFoo functionPtr = &foo;
    return functionPtr;
}
int main() {
  int (*functionNoPtr)(int,int);
  functionNoPtr = foo;
  int sum = functionNoPtr(1, 3);
  ((sum == 4) ? (void) (0) : __assert_fail ("sum == 4", "func_ptr_call_test.c", 78, __extension__ __PRETTY_FUNCTION__));
  sum = bar(functionNoPtr);
  ((sum == 88890) ? (void) (0) : __assert_fail ("sum == 88890", "func_ptr_call_test.c", 82, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(functionNoPtr, 2, 5);
  ((sum == 7) ? (void) (0) : __assert_fail ("sum == 7", "func_ptr_call_test.c", 86, __extension__ __PRETTY_FUNCTION__));
  int (*functionPtr)(int,int);
  functionPtr = &foo;
  sum = functionPtr(3, 7);
  ((sum == 10) ? (void) (0) : __assert_fail ("sum == 10", "func_ptr_call_test.c", 92, __extension__ __PRETTY_FUNCTION__));
  sum = bar(functionPtr);
  ((sum == 88892) ? (void) (0) : __assert_fail ("sum == 88892", "func_ptr_call_test.c", 96, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(functionPtr, 4, 9);
  ((sum == 13) ? (void) (0) : __assert_fail ("sum == 13", "func_ptr_call_test.c", 100, __extension__ __PRETTY_FUNCTION__));
  sum = (*functionPtr)(5, 11);
  ((sum == 16) ? (void) (0) : __assert_fail ("sum == 16", "func_ptr_call_test.c", 104, __extension__ __PRETTY_FUNCTION__));
  functionPtr = functionFunction();
  sum = (*functionPtr)(6, 13);
  ((sum == 19) ? (void) (0) : __assert_fail ("sum == 19", "func_ptr_call_test.c", 109, __extension__ __PRETTY_FUNCTION__));
  sum = bar(functionPtr);
  ((sum == 88894) ? (void) (0) : __assert_fail ("sum == 88894", "func_ptr_call_test.c", 113, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(functionPtr, 7, 15);
  ((sum == 22) ? (void) (0) : __assert_fail ("sum == 22", "func_ptr_call_test.c", 117, __extension__ __PRETTY_FUNCTION__));
  FuncDefFoo funcDefFoo = functionFunction();
  sum = (*funcDefFoo)(8, 17);
  ((sum == 25) ? (void) (0) : __assert_fail ("sum == 25", "func_ptr_call_test.c", 122, __extension__ __PRETTY_FUNCTION__));
  sum = (funcDefFoo)(9, 19);
  ((sum == 28) ? (void) (0) : __assert_fail ("sum == 28", "func_ptr_call_test.c", 126, __extension__ __PRETTY_FUNCTION__));
  sum = bar(funcDefFoo);
  ((sum == 88896) ? (void) (0) : __assert_fail ("sum == 88896", "func_ptr_call_test.c", 130, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(funcDefFoo, 10, 21);
  ((sum == 31) ? (void) (0) : __assert_fail ("sum == 31", "func_ptr_call_test.c", 134, __extension__ __PRETTY_FUNCTION__));
  struct fooStruct * fooPtr = malloc(sizeof(struct fooStruct));
  if (fooPtr == 0) {
    return 0;
  }
  fooPtr->function = &foo;
  sum = fooPtr->function(11, 23);
  ((sum == 34) ? (void) (0) : __assert_fail ("sum == 34", "func_ptr_call_test.c", 145, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(fooPtr->function, 12, 25);
  ((sum == 37) ? (void) (0) : __assert_fail ("sum == 37", "func_ptr_call_test.c", 149, __extension__ __PRETTY_FUNCTION__));
  struct fooStruct * fooPointa = malloc(sizeof(struct fooStruct));
  if (fooPointa == 0) {
    return 0;
  }
  fooPointa->function = foo;
  sum = fooPointa->function(13, 27);
  ((sum == 40) ? (void) (0) : __assert_fail ("sum == 40", "func_ptr_call_test.c", 158, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(fooPointa->function, 14, 29);
  ((sum == 43) ? (void) (0) : __assert_fail ("sum == 43", "func_ptr_call_test.c", 162, __extension__ __PRETTY_FUNCTION__));
  struct barStruct * barPtr = malloc(sizeof(struct barStruct));
  if (barPtr == 0) {
    return 0;
  }
  barPtr->bar = fooPtr;
  sum = barPtr->bar->function(15, 31);
  ((sum == 46) ? (void) (0) : __assert_fail ("sum == 46", "func_ptr_call_test.c", 171, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(barPtr->bar->function, 16, 33);
  ((sum == 49) ? (void) (0) : __assert_fail ("sum == 49", "func_ptr_call_test.c", 175, __extension__ __PRETTY_FUNCTION__));
  struct barStruct * barPointa = malloc(sizeof(struct barStruct));
  if (barPointa == 0) {
    return 0;
  }
  barPointa->bar = fooPointa;
  sum = barPointa->bar->function(17, 35);
  ((sum == 52) ? (void) (0) : __assert_fail ("sum == 52", "func_ptr_call_test.c", 184, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(barPointa->bar->function, 18, 37);
  ((sum == 55) ? (void) (0) : __assert_fail ("sum == 55", "func_ptr_call_test.c", 188, __extension__ __PRETTY_FUNCTION__));
  struct barStruct localbarPtr1;
  localbarPtr1.bar = fooPtr;
  sum = localbarPtr1.bar->function(19, 39);
  ((sum == 58) ? (void) (0) : __assert_fail ("sum == 58", "func_ptr_call_test.c", 194, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(barPtr->bar->function, 20, 41);
  ((sum == 61) ? (void) (0) : __assert_fail ("sum == 61", "func_ptr_call_test.c", 198, __extension__ __PRETTY_FUNCTION__));
  struct barStruct localBarPtr2;
  localBarPtr2.bar = fooPointa;
  sum = localBarPtr2.bar->function(21, 43);
  ((sum == 64) ? (void) (0) : __assert_fail ("sum == 64", "func_ptr_call_test.c", 204, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(localBarPtr2.bar->function, 23, 45);
  ((sum == 68) ? (void) (0) : __assert_fail ("sum == 68", "func_ptr_call_test.c", 208, __extension__ __PRETTY_FUNCTION__));
  struct fooStruct localFooStruct1;
  localFooStruct1.function = &foo;
  sum = localFooStruct1.function(24, 47);
  ((sum == 71) ? (void) (0) : __assert_fail ("sum == 71", "func_ptr_call_test.c", 215, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(localFooStruct1.function, 25, 49);
  ((sum == 74) ? (void) (0) : __assert_fail ("sum == 74", "func_ptr_call_test.c", 219, __extension__ __PRETTY_FUNCTION__));
  struct fooStruct localFooStruct2;
  localFooStruct2.function = foo;
  sum = localFooStruct2.function(26, 51);
  ((sum == 77) ? (void) (0) : __assert_fail ("sum == 77", "func_ptr_call_test.c", 225, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(localFooStruct2.function, 27, 53);
  ((sum == 80) ? (void) (0) : __assert_fail ("sum == 80", "func_ptr_call_test.c", 229, __extension__ __PRETTY_FUNCTION__));
  struct localBarStruct localbar1;
  localbar1.bar = localFooStruct1;
  sum = localbar1.bar.function(28, 55);
  ((sum == 83) ? (void) (0) : __assert_fail ("sum == 83", "func_ptr_call_test.c", 235, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(localbar1.bar.function, 29, 57);
  ((sum == 86) ? (void) (0) : __assert_fail ("sum == 86", "func_ptr_call_test.c", 239, __extension__ __PRETTY_FUNCTION__));
  struct localBarStruct localBar2;
  localBar2.bar = localFooStruct2;
  sum = localBar2.bar.function(30, 59);
  ((sum == 89) ? (void) (0) : __assert_fail ("sum == 89", "func_ptr_call_test.c", 245, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(localBar2.bar.function, 31, 61);
  ((sum == 92) ? (void) (0) : __assert_fail ("sum == 92", "func_ptr_call_test.c", 249, __extension__ __PRETTY_FUNCTION__));
  struct funcDefFooStruct * funcDefFooPtr = malloc(sizeof(renamedFuncDefFooStruct));
  if (funcDefFooPtr == 0) {
    return 0;
  }
  funcDefFooPtr->localFoo = &foo;
  sum = funcDefFooPtr->localFoo(32, 63);
  ((sum == 95) ? (void) (0) : __assert_fail ("sum == 95", "func_ptr_call_test.c", 259, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(funcDefFooPtr->localFoo, 33, 65);
  ((sum == 98) ? (void) (0) : __assert_fail ("sum == 98", "func_ptr_call_test.c", 263, __extension__ __PRETTY_FUNCTION__));
  funcDefFooPtr->funcDefFooPtrRef = &(funcDefFooPtr->localFoo);
  sum = (*(funcDefFooPtr->funcDefFooPtrRef))(34, 67);
  ((sum == 101) ? (void) (0) : __assert_fail ("sum == 101", "func_ptr_call_test.c", 269, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*(funcDefFooPtr->funcDefFooPtrRef)), 35, 69);
  ((sum == 104) ? (void) (0) : __assert_fail ("sum == 104", "func_ptr_call_test.c", 273, __extension__ __PRETTY_FUNCTION__));
  funcDefFooPtr->barStructRef = localbarPtr1;
  sum = funcDefFooPtr->barStructRef.bar->function(36, 71);
  ((sum == 107) ? (void) (0) : __assert_fail ("sum == 107", "func_ptr_call_test.c", 278, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(funcDefFooPtr->barStructRef.bar->function, 37, 73);
  ((sum == 110) ? (void) (0) : __assert_fail ("sum == 110", "func_ptr_call_test.c", 282, __extension__ __PRETTY_FUNCTION__));
  funcDefFooPtr->localBarStructRef = localbar1;
  sum = funcDefFooPtr->localBarStructRef.bar.function(38, 75);
  ((sum == 113) ? (void) (0) : __assert_fail ("sum == 113", "func_ptr_call_test.c", 287, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(funcDefFooPtr->localBarStructRef.bar.function, 39, 77);
  ((sum == 116) ? (void) (0) : __assert_fail ("sum == 116", "func_ptr_call_test.c", 291, __extension__ __PRETTY_FUNCTION__));
  funcDefFooPtr->barStructPtrRef = &localbarPtr1;
  funcDefFooPtr->localBarStructPtrRef = &localbar1;
  sum = funcDefFooPtr->barStructPtrRef->bar->function(40, 79);
  ((sum == 119) ? (void) (0) : __assert_fail ("sum == 119", "func_ptr_call_test.c", 298, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(funcDefFooPtr->barStructPtrRef->bar->function, 41, 81);
  ((sum == 122) ? (void) (0) : __assert_fail ("sum == 122", "func_ptr_call_test.c", 302, __extension__ __PRETTY_FUNCTION__));
  sum = funcDefFooPtr->localBarStructPtrRef->bar.function(42, 83);
  ((sum == 125) ? (void) (0) : __assert_fail ("sum == 125", "func_ptr_call_test.c", 306, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(funcDefFooPtr->localBarStructPtrRef->bar.function, 53, 85);
  ((sum == 138) ? (void) (0) : __assert_fail ("sum == 138", "func_ptr_call_test.c", 310, __extension__ __PRETTY_FUNCTION__));
  combiStruct.barStructRef = localBarPtr2;
  sum = combiStruct.barStructRef.bar->function(88, 188);
  ((sum == 276) ? (void) (0) : __assert_fail ("sum == 276", "func_ptr_call_test.c", 317, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(combiStruct.barStructRef.bar->function, 89, 191);
  ((sum == 280) ? (void) (0) : __assert_fail ("sum == 280", "func_ptr_call_test.c", 321, __extension__ __PRETTY_FUNCTION__));
  FuncDefFoo bonusVar;
  bonusVar = foo;
  combiStruct.funcDefFooPtrRef = &bonusVar;
  sum = (*(combiStruct.funcDefFooPtrRef))(90, 194);
  ((sum == 284) ? (void) (0) : __assert_fail ("sum == 284", "func_ptr_call_test.c", 328, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*(combiStruct.funcDefFooPtrRef)), 91, 197);
  ((sum == 288) ? (void) (0) : __assert_fail ("sum == 288", "func_ptr_call_test.c", 332, __extension__ __PRETTY_FUNCTION__));
  combiStruct.localBarStructRef = localBar2;
  sum = combiStruct.localBarStructRef.bar.function(92, 200);
  ((sum == 292) ? (void) (0) : __assert_fail ("sum == 292", "func_ptr_call_test.c", 337, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(combiStruct.localBarStructRef.bar.function, 93, 203);
  ((sum == 296) ? (void) (0) : __assert_fail ("sum == 296", "func_ptr_call_test.c", 341, __extension__ __PRETTY_FUNCTION__));
  combiStruct.localFoo = foo;
  sum = combiStruct.localFoo(94, 206);
  ((sum == 300) ? (void) (0) : __assert_fail ("sum == 300", "func_ptr_call_test.c", 346, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(combiStruct.localFoo, 95, 209);
  ((sum == 304) ? (void) (0) : __assert_fail ("sum == 304", "func_ptr_call_test.c", 350, __extension__ __PRETTY_FUNCTION__));
  combiStruct.barStructPtrRef = &localBarPtr2;
  sum = combiStruct.barStructPtrRef->bar->function(96, 212);
  ((sum == 308) ? (void) (0) : __assert_fail ("sum == 308", "func_ptr_call_test.c", 355, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(combiStruct.barStructPtrRef->bar->function, 97, 215);
  ((sum == 312) ? (void) (0) : __assert_fail ("sum == 312", "func_ptr_call_test.c", 359, __extension__ __PRETTY_FUNCTION__));
  combiStruct.localBarStructPtrRef = &localBar2;
  sum = combiStruct.localBarStructPtrRef->bar.function(98, 218);
  ((sum == 316) ? (void) (0) : __assert_fail ("sum == 316", "func_ptr_call_test.c", 364, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(combiStruct.localBarStructPtrRef->bar.function, 99, 221);
  ((sum == 320) ? (void) (0) : __assert_fail ("sum == 320", "func_ptr_call_test.c", 368, __extension__ __PRETTY_FUNCTION__));
  int (*barBarArray[2])(int (*)(int, int), int, int) = {&barBar, barBar};
  sum = (*barBarArray[0])(combiStruct.localBarStructRef.bar.function, 100, 211232);
  ((sum == 211332) ? (void) (0) : __assert_fail ("sum == 211332", "func_ptr_call_test.c", 377, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(combiStruct.localBarStructPtrRef->bar.function, 101, 2231);
  ((sum == 2332) ? (void) (0) : __assert_fail ("sum == 2332", "func_ptr_call_test.c", 381, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(combiStruct.barStructPtrRef->bar->function, 102, 21909);
  ((sum == 22011) ? (void) (0) : __assert_fail ("sum == 22011", "func_ptr_call_test.c", 385, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(combiStruct.localFoo, 103, 210099);
  ((sum == 210202) ? (void) (0) : __assert_fail ("sum == 210202", "func_ptr_call_test.c", 389, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(combiStruct.barStructRef.bar->function, 104, 214554);
  ((sum == 214658) ? (void) (0) : __assert_fail ("sum == 214658", "func_ptr_call_test.c", 393, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*(combiStruct.funcDefFooPtrRef)), 105, 2198724);
  ((sum == 2198829) ? (void) (0) : __assert_fail ("sum == 2198829", "func_ptr_call_test.c", 397, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(funcDefFooPtr->localFoo, 106, 214562);
  ((sum == 214668) ? (void) (0) : __assert_fail ("sum == 214668", "func_ptr_call_test.c", 401, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*(funcDefFooPtr->funcDefFooPtrRef)), 107, 211544);
  ((sum == 211651) ? (void) (0) : __assert_fail ("sum == 211651", "func_ptr_call_test.c", 405, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(funcDefFooPtr->barStructRef.bar->function, 108, 219536);
  ((sum == 219644) ? (void) (0) : __assert_fail ("sum == 219644", "func_ptr_call_test.c", 409, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(funcDefFooPtr->localBarStructRef.bar.function, 109, 21124);
  ((sum == 21233) ? (void) (0) : __assert_fail ("sum == 21233", "func_ptr_call_test.c", 413, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(funcDefFooPtr->barStructPtrRef->bar->function, 110, 213656);
  ((sum == 213766) ? (void) (0) : __assert_fail ("sum == 213766", "func_ptr_call_test.c", 417, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(funcDefFooPtr->localBarStructPtrRef->bar.function, 111, 2154454);
  ((sum == 2154565) ? (void) (0) : __assert_fail ("sum == 2154565", "func_ptr_call_test.c", 421, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(combiStruct.localBarStructRef.bar.function, 112, 211312);
  ((sum == 211424) ? (void) (0) : __assert_fail ("sum == 211424", "func_ptr_call_test.c", 426, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(combiStruct.localBarStructPtrRef->bar.function, 113, 217999);
  ((sum == 218112) ? (void) (0) : __assert_fail ("sum == 218112", "func_ptr_call_test.c", 430, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(combiStruct.barStructPtrRef->bar->function, 114, 2178788);
  ((sum == 2178902) ? (void) (0) : __assert_fail ("sum == 2178902", "func_ptr_call_test.c", 434, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(combiStruct.localFoo, 115, 217878);
  ((sum == 217993) ? (void) (0) : __assert_fail ("sum == 217993", "func_ptr_call_test.c", 438, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(combiStruct.barStructRef.bar->function, 116, 2167677);
  ((sum == 2167793) ? (void) (0) : __assert_fail ("sum == 2167793", "func_ptr_call_test.c", 442, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*(combiStruct.funcDefFooPtrRef)), 117, 216767);
  ((sum == 216884) ? (void) (0) : __assert_fail ("sum == 216884", "func_ptr_call_test.c", 446, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(funcDefFooPtr->localFoo, 118, 21565666);
  ((sum == 21565784) ? (void) (0) : __assert_fail ("sum == 21565784", "func_ptr_call_test.c", 450, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*(funcDefFooPtr->funcDefFooPtrRef)), 119, 215656);
  ((sum == 215775) ? (void) (0) : __assert_fail ("sum == 215775", "func_ptr_call_test.c", 454, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(funcDefFooPtr->barStructRef.bar->function, 120, 21454555);
  ((sum == 21454675) ? (void) (0) : __assert_fail ("sum == 21454675", "func_ptr_call_test.c", 458, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(funcDefFooPtr->localBarStructRef.bar.function, 121, 214545);
  ((sum == 214666) ? (void) (0) : __assert_fail ("sum == 214666", "func_ptr_call_test.c", 462, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(funcDefFooPtr->barStructPtrRef->bar->function, 122, 212323333);
  ((sum == 212323455) ? (void) (0) : __assert_fail ("sum == 212323455", "func_ptr_call_test.c", 466, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(funcDefFooPtr->localBarStructPtrRef->bar.function, 123, 2123233);
  ((sum == 2123356) ? (void) (0) : __assert_fail ("sum == 2123356", "func_ptr_call_test.c", 470, __extension__ __PRETTY_FUNCTION__));
  int (*funArray[3]) (int, int);
  funArray[0] = functionPtr;
  funArray[1] = funcDefFoo;
  funArray[2] = foo;
  sum = (*funArray[0]) (1109, 2200);
  ((sum == 3309) ? (void) (0) : __assert_fail ("sum == 3309", "func_ptr_call_test.c", 482, __extension__ __PRETTY_FUNCTION__));
  sum = (*funArray[1]) (1110, 2211);
  ((sum == 3321) ? (void) (0) : __assert_fail ("sum == 3321", "func_ptr_call_test.c", 486, __extension__ __PRETTY_FUNCTION__));
  sum = (*funArray[2]) (1111, 2222);
  ((sum == 3333) ? (void) (0) : __assert_fail ("sum == 3333", "func_ptr_call_test.c", 490, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(funArray[0], 93, 203);
  ((sum == 296) ? (void) (0) : __assert_fail ("sum == 296", "func_ptr_call_test.c", 494, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(funArray[1], 94, 203);
  ((sum == 297) ? (void) (0) : __assert_fail ("sum == 297", "func_ptr_call_test.c", 498, __extension__ __PRETTY_FUNCTION__));
  sum = barBar(funArray[2], 95, 203);
  ((sum == 298) ? (void) (0) : __assert_fail ("sum == 298", "func_ptr_call_test.c", 502, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(funArray[0], 1067, 211);
  ((sum == 1278) ? (void) (0) : __assert_fail ("sum == 1278", "func_ptr_call_test.c", 506, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(funArray[1], 1056, 212);
  ((sum == 1268) ? (void) (0) : __assert_fail ("sum == 1268", "func_ptr_call_test.c", 510, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])(funArray[2], 1045, 213);
  ((sum == 1258) ? (void) (0) : __assert_fail ("sum == 1258", "func_ptr_call_test.c", 514, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(funArray[0], 1034, 214);
  ((sum == 1248) ? (void) (0) : __assert_fail ("sum == 1248", "func_ptr_call_test.c", 519, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(funArray[1], 1023, 215);
  ((sum == 1238) ? (void) (0) : __assert_fail ("sum == 1238", "func_ptr_call_test.c", 523, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])(funArray[2], 1012, 216);
  ((sum == 1228) ? (void) (0) : __assert_fail ("sum == 1228", "func_ptr_call_test.c", 527, __extension__ __PRETTY_FUNCTION__));
  struct boah {
    struct funcDefFooStruct * ptrRef;
    struct funcDefFooStruct localRef;
    renamedFuncDefFooStruct * ptrRefRenamed;
    renamedFuncDefFooStruct localRefRenamed;
  } dasLetzteStruct = {funcDefFooPtr, combiStruct, (renamedFuncDefFooStruct *) funcDefFooPtr, (renamedFuncDefFooStruct) combiStruct};
  struct boah * dasLetzteStructPtr = &dasLetzteStruct;
  struct boah * dasLetzteStructPtrArray[1];
  dasLetzteStructPtrArray[0] = dasLetzteStructPtr;
  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->localFoo(200, 15353535);
  ((sum == 15353735) ? (void) (0) : __assert_fail ("sum == 15353735", "func_ptr_call_test.c", 547, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localFoo, 201, 21232323);
  ((sum == 21232524) ? (void) (0) : __assert_fail ("sum == 21232524", "func_ptr_call_test.c", 551, __extension__ __PRETTY_FUNCTION__));
  sum = (*((*dasLetzteStructPtrArray[0]).ptrRefRenamed->funcDefFooPtrRef))(202, 15121212);
  ((sum == 15121414) ? (void) (0) : __assert_fail ("sum == 15121414", "func_ptr_call_test.c", 555, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*((*dasLetzteStructPtrArray[0]).ptrRefRenamed->funcDefFooPtrRef)), 203, 219999);
  ((sum == 220202) ? (void) (0) : __assert_fail ("sum == 220202", "func_ptr_call_test.c", 559, __extension__ __PRETTY_FUNCTION__));
  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructRef.bar->function(204, 158888);
  ((sum == 159092) ? (void) (0) : __assert_fail ("sum == 159092", "func_ptr_call_test.c", 563, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructRef.bar->function, 205, 217777);
  ((sum == 217982) ? (void) (0) : __assert_fail ("sum == 217982", "func_ptr_call_test.c", 567, __extension__ __PRETTY_FUNCTION__));
  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructRef.bar.function(206, 156666);
  ((sum == 156872) ? (void) (0) : __assert_fail ("sum == 156872", "func_ptr_call_test.c", 571, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructRef.bar.function, 207, 215555);
  ((sum == 215762) ? (void) (0) : __assert_fail ("sum == 215762", "func_ptr_call_test.c", 575, __extension__ __PRETTY_FUNCTION__));
  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructPtrRef->bar->function(208, 150000);
  ((sum == 150208) ? (void) (0) : __assert_fail ("sum == 150208", "func_ptr_call_test.c", 579, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructPtrRef->bar->function, 209, 211111);
  ((sum == 211320) ? (void) (0) : __assert_fail ("sum == 211320", "func_ptr_call_test.c", 583, __extension__ __PRETTY_FUNCTION__));
  sum = (*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructPtrRef->bar.function(210, 152222);
  ((sum == 152432) ? (void) (0) : __assert_fail ("sum == 152432", "func_ptr_call_test.c", 587, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructPtrRef->bar.function, 211, 213333);
  ((sum == 213544) ? (void) (0) : __assert_fail ("sum == 213544", "func_ptr_call_test.c", 591, __extension__ __PRETTY_FUNCTION__));
  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.barStructRef.bar->function(212, 188);
  ((sum == 400) ? (void) (0) : __assert_fail ("sum == 400", "func_ptr_call_test.c", 596, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructRef.bar->function, 213, 191);
  ((sum == 404) ? (void) (0) : __assert_fail ("sum == 404", "func_ptr_call_test.c", 600, __extension__ __PRETTY_FUNCTION__));
  sum = (*((*dasLetzteStructPtrArray[0]).localRefRenamed.funcDefFooPtrRef))(90, 194);
  ((sum == 284) ? (void) (0) : __assert_fail ("sum == 284", "func_ptr_call_test.c", 604, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*((*dasLetzteStructPtrArray[0]).localRefRenamed.funcDefFooPtrRef)), 91, 197);
  ((sum == 288) ? (void) (0) : __assert_fail ("sum == 288", "func_ptr_call_test.c", 608, __extension__ __PRETTY_FUNCTION__));
  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructRef.bar.function(92, 200);
  ((sum == 292) ? (void) (0) : __assert_fail ("sum == 292", "func_ptr_call_test.c", 612, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructRef.bar.function, 93, 203);
  ((sum == 296) ? (void) (0) : __assert_fail ("sum == 296", "func_ptr_call_test.c", 616, __extension__ __PRETTY_FUNCTION__));
  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.localFoo(94, 206);
  ((sum == 300) ? (void) (0) : __assert_fail ("sum == 300", "func_ptr_call_test.c", 620, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.localFoo, 95, 209);
  ((sum == 304) ? (void) (0) : __assert_fail ("sum == 304", "func_ptr_call_test.c", 624, __extension__ __PRETTY_FUNCTION__));
  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.barStructPtrRef->bar->function(96, 212);
  ((sum == 308) ? (void) (0) : __assert_fail ("sum == 308", "func_ptr_call_test.c", 628, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructPtrRef->bar->function, 97, 215);
  ((sum == 312) ? (void) (0) : __assert_fail ("sum == 312", "func_ptr_call_test.c", 632, __extension__ __PRETTY_FUNCTION__));
  sum = (*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructPtrRef->bar.function(98, 218);
  ((sum == 316) ? (void) (0) : __assert_fail ("sum == 316", "func_ptr_call_test.c", 636, __extension__ __PRETTY_FUNCTION__));
  sum = barBar((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructPtrRef->bar.function, 99, 221);
  ((sum == 320) ? (void) (0) : __assert_fail ("sum == 320", "func_ptr_call_test.c", 640, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructRef.bar.function, 300, 211);
  ((sum == 511) ? (void) (0) : __assert_fail ("sum == 511", "func_ptr_call_test.c", 645, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructPtrRef->bar.function, 301, 212);
  ((sum == 513) ? (void) (0) : __assert_fail ("sum == 513", "func_ptr_call_test.c", 649, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructPtrRef->bar->function, 302, 213);
  ((sum == 515) ? (void) (0) : __assert_fail ("sum == 515", "func_ptr_call_test.c", 653, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.localFoo, 303, 214);
  ((sum == 517) ? (void) (0) : __assert_fail ("sum == 517", "func_ptr_call_test.c", 657, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructRef.bar->function, 304, 215);
  ((sum == 519) ? (void) (0) : __assert_fail ("sum == 519", "func_ptr_call_test.c", 661, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*((*dasLetzteStructPtrArray[0]).localRefRenamed.funcDefFooPtrRef)), 305, 216);
  ((sum == 521) ? (void) (0) : __assert_fail ("sum == 521", "func_ptr_call_test.c", 665, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localFoo, 306, 217);
  ((sum == 523) ? (void) (0) : __assert_fail ("sum == 523", "func_ptr_call_test.c", 669, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*((*dasLetzteStructPtrArray[0]).ptrRefRenamed->funcDefFooPtrRef)), 307, 218);
  ((sum == 525) ? (void) (0) : __assert_fail ("sum == 525", "func_ptr_call_test.c", 673, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructRef.bar->function, 308, 219);
  ((sum == 527) ? (void) (0) : __assert_fail ("sum == 527", "func_ptr_call_test.c", 677, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructRef.bar.function, 309, 2100);
  ((sum == 2409) ? (void) (0) : __assert_fail ("sum == 2409", "func_ptr_call_test.c", 681, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructPtrRef->bar->function, 310, 2111);
  ((sum == 2421) ? (void) (0) : __assert_fail ("sum == 2421", "func_ptr_call_test.c", 685, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[0])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructPtrRef->bar.function, 311, 2122);
  ((sum == 2433) ? (void) (0) : __assert_fail ("sum == 2433", "func_ptr_call_test.c", 689, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructRef.bar.function, 312, 2133);
  ((sum == 2445) ? (void) (0) : __assert_fail ("sum == 2445", "func_ptr_call_test.c", 694, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.localBarStructPtrRef->bar.function, 313, 2144);
  ((sum == 2457) ? (void) (0) : __assert_fail ("sum == 2457", "func_ptr_call_test.c", 698, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructPtrRef->bar->function, 314, 2155);
  ((sum == 2469) ? (void) (0) : __assert_fail ("sum == 2469", "func_ptr_call_test.c", 702, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.localFoo, 315, 2166);
  ((sum == 2481) ? (void) (0) : __assert_fail ("sum == 2481", "func_ptr_call_test.c", 706, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).localRefRenamed.barStructRef.bar->function, 316, 2177);
  ((sum == 2493) ? (void) (0) : __assert_fail ("sum == 2493", "func_ptr_call_test.c", 710, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*((*dasLetzteStructPtrArray[0]).localRefRenamed.funcDefFooPtrRef)), 317, 2188);
  ((sum == 2505) ? (void) (0) : __assert_fail ("sum == 2505", "func_ptr_call_test.c", 714, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localFoo, 318, 2199);
  ((sum == 2517) ? (void) (0) : __assert_fail ("sum == 2517", "func_ptr_call_test.c", 718, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*((*dasLetzteStructPtrArray[0]).ptrRefRenamed->funcDefFooPtrRef)), 319, 21111);
  ((sum == 21430) ? (void) (0) : __assert_fail ("sum == 21430", "func_ptr_call_test.c", 722, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructRef.bar->function, 320, 21222);
  ((sum == 21542) ? (void) (0) : __assert_fail ("sum == 21542", "func_ptr_call_test.c", 726, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructRef.bar.function, 321, 21333);
  ((sum == 21654) ? (void) (0) : __assert_fail ("sum == 21654", "func_ptr_call_test.c", 730, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->barStructPtrRef->bar->function, 322, 21444);
  ((sum == 21766) ? (void) (0) : __assert_fail ("sum == 21766", "func_ptr_call_test.c", 734, __extension__ __PRETTY_FUNCTION__));
  sum = (*barBarArray[1])((*dasLetzteStructPtrArray[0]).ptrRefRenamed->localBarStructPtrRef->bar.function, 323, 21555);
  ((sum == 21878) ? (void) (0) : __assert_fail ("sum == 21878", "func_ptr_call_test.c", 738, __extension__ __PRETTY_FUNCTION__));
  return 0;
}
