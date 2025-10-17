// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert_perror_fail (int __errnum, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));


typedef unsigned int size_t;
typedef __builtin_va_list __gnuc_va_list;
typedef unsigned char __u_char;
typedef unsigned short int __u_short;
typedef unsigned int __u_int;
typedef unsigned long int __u_long;
typedef signed char __int8_t;
typedef unsigned char __uint8_t;
typedef signed short int __int16_t;
typedef unsigned short int __uint16_t;
typedef signed int __int32_t;
typedef unsigned int __uint32_t;
__extension__ typedef signed long long int __int64_t;
__extension__ typedef unsigned long long int __uint64_t;
typedef __int8_t __int_least8_t;
typedef __uint8_t __uint_least8_t;
typedef __int16_t __int_least16_t;
typedef __uint16_t __uint_least16_t;
typedef __int32_t __int_least32_t;
typedef __uint32_t __uint_least32_t;
typedef __int64_t __int_least64_t;
typedef __uint64_t __uint_least64_t;
__extension__ typedef long long int __quad_t;
__extension__ typedef unsigned long long int __u_quad_t;
__extension__ typedef long long int __intmax_t;
__extension__ typedef unsigned long long int __uintmax_t;
__extension__ typedef __uint64_t __dev_t;
__extension__ typedef unsigned int __uid_t;
__extension__ typedef unsigned int __gid_t;
__extension__ typedef unsigned long int __ino_t;
__extension__ typedef __uint64_t __ino64_t;
__extension__ typedef unsigned int __mode_t;
__extension__ typedef unsigned int __nlink_t;
__extension__ typedef long int __off_t;
__extension__ typedef __int64_t __off64_t;
__extension__ typedef int __pid_t;
__extension__ typedef struct { int __val[2]; } __fsid_t;
__extension__ typedef long int __clock_t;
__extension__ typedef unsigned long int __rlim_t;
__extension__ typedef __uint64_t __rlim64_t;
__extension__ typedef unsigned int __id_t;
__extension__ typedef long int __time_t;
__extension__ typedef unsigned int __useconds_t;
__extension__ typedef long int __suseconds_t;
__extension__ typedef __int64_t __suseconds64_t;
__extension__ typedef int __daddr_t;
__extension__ typedef int __key_t;
__extension__ typedef int __clockid_t;
__extension__ typedef void * __timer_t;
__extension__ typedef long int __blksize_t;
__extension__ typedef long int __blkcnt_t;
__extension__ typedef __int64_t __blkcnt64_t;
__extension__ typedef unsigned long int __fsblkcnt_t;
__extension__ typedef __uint64_t __fsblkcnt64_t;
__extension__ typedef unsigned long int __fsfilcnt_t;
__extension__ typedef __uint64_t __fsfilcnt64_t;
__extension__ typedef int __fsword_t;
__extension__ typedef int __ssize_t;
__extension__ typedef long int __syscall_slong_t;
__extension__ typedef unsigned long int __syscall_ulong_t;
typedef __off64_t __loff_t;
typedef char *__caddr_t;
__extension__ typedef int __intptr_t;
__extension__ typedef unsigned int __socklen_t;
typedef int __sig_atomic_t;
__extension__ typedef __int64_t __time64_t;
typedef struct
{
  int __count;
  union
  {
    unsigned int __wch;
    char __wchb[4];
  } __value;
} __mbstate_t;
typedef struct _G_fpos_t
{
  __off_t __pos;
  __mbstate_t __state;
} __fpos_t;
typedef struct _G_fpos64_t
{
  __off64_t __pos;
  __mbstate_t __state;
} __fpos64_t;
struct _IO_FILE;
typedef struct _IO_FILE __FILE;
struct _IO_FILE;
typedef struct _IO_FILE FILE;
struct _IO_FILE;
struct _IO_marker;
struct _IO_codecvt;
struct _IO_wide_data;
typedef void _IO_lock_t;
struct _IO_FILE
{
  int _flags;
  char *_IO_read_ptr;
  char *_IO_read_end;
  char *_IO_read_base;
  char *_IO_write_base;
  char *_IO_write_ptr;
  char *_IO_write_end;
  char *_IO_buf_base;
  char *_IO_buf_end;
  char *_IO_save_base;
  char *_IO_backup_base;
  char *_IO_save_end;
  struct _IO_marker *_markers;
  struct _IO_FILE *_chain;
  int _fileno;
  int _flags2;
  __off_t _old_offset;
  unsigned short _cur_column;
  signed char _vtable_offset;
  char _shortbuf[1];
  _IO_lock_t *_lock;
  __off64_t _offset;
  struct _IO_codecvt *_codecvt;
  struct _IO_wide_data *_wide_data;
  struct _IO_FILE *_freeres_list;
  void *_freeres_buf;
  size_t __pad5;
  int _mode;
  char _unused2[15 * sizeof (int) - 4 * sizeof (void *) - sizeof (size_t)];
};
typedef __fpos_t fpos_t;
extern FILE *stdin;
extern FILE *stdout;
extern FILE *stderr;
extern int remove (const char *__filename) __attribute__ ((__nothrow__ , __leaf__));
extern int rename (const char *__old, const char *__new) __attribute__ ((__nothrow__ , __leaf__));
extern int fclose (FILE *__stream) __attribute__ ((__nonnull__ (1)));
extern FILE *tmpfile (void)
  __attribute__ ((__malloc__)) __attribute__ ((__malloc__ (fclose, 1))) ;
extern char *tmpnam (char[20]) __attribute__ ((__nothrow__ , __leaf__)) ;
extern int fflush (FILE *__stream);
extern FILE *fopen (const char *__restrict __filename,
      const char *__restrict __modes)
  __attribute__ ((__malloc__)) __attribute__ ((__malloc__ (fclose, 1))) ;
extern FILE *freopen (const char *__restrict __filename,
        const char *__restrict __modes,
        FILE *__restrict __stream) __attribute__ ((__nonnull__ (3)));
extern void setbuf (FILE *__restrict __stream, char *__restrict __buf) __attribute__ ((__nothrow__ , __leaf__))
  __attribute__ ((__nonnull__ (1)));
extern int setvbuf (FILE *__restrict __stream, char *__restrict __buf,
      int __modes, size_t __n) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int fprintf (FILE *__restrict __stream,
      const char *__restrict __format, ...) __attribute__ ((__nonnull__ (1)));
extern int printf (const char *__restrict __format, ...);
extern int sprintf (char *__restrict __s,
      const char *__restrict __format, ...) __attribute__ ((__nothrow__));
extern int vfprintf (FILE *__restrict __s, const char *__restrict __format,
       __gnuc_va_list __arg) __attribute__ ((__nonnull__ (1)));
extern int vprintf (const char *__restrict __format, __gnuc_va_list __arg);
extern int vsprintf (char *__restrict __s, const char *__restrict __format,
       __gnuc_va_list __arg) __attribute__ ((__nothrow__));
extern int snprintf (char *__restrict __s, size_t __maxlen,
       const char *__restrict __format, ...)
     __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__printf__, 3, 4)));
extern int vsnprintf (char *__restrict __s, size_t __maxlen,
        const char *__restrict __format, __gnuc_va_list __arg)
     __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__printf__, 3, 0)));
extern int fscanf (FILE *__restrict __stream,
     const char *__restrict __format, ...) __attribute__ ((__nonnull__ (1)));
extern int scanf (const char *__restrict __format, ...) ;
extern int sscanf (const char *__restrict __s,
     const char *__restrict __format, ...) __attribute__ ((__nothrow__ , __leaf__));
extern int fscanf (FILE *__restrict __stream, const char *__restrict __format, ...) __asm__ ("" "__isoc99_fscanf") __attribute__ ((__nonnull__ (1)));
extern int scanf (const char *__restrict __format, ...) __asm__ ("" "__isoc99_scanf") ;
extern int sscanf (const char *__restrict __s, const char *__restrict __format, ...) __asm__ ("" "__isoc99_sscanf") __attribute__ ((__nothrow__ , __leaf__));
extern int vfscanf (FILE *__restrict __s, const char *__restrict __format,
      __gnuc_va_list __arg)
     __attribute__ ((__format__ (__scanf__, 2, 0))) __attribute__ ((__nonnull__ (1)));
extern int vscanf (const char *__restrict __format, __gnuc_va_list __arg)
     __attribute__ ((__format__ (__scanf__, 1, 0))) ;
extern int vsscanf (const char *__restrict __s,
      const char *__restrict __format, __gnuc_va_list __arg)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__format__ (__scanf__, 2, 0)));
extern int vfscanf (FILE *__restrict __s, const char *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vfscanf")
     __attribute__ ((__format__ (__scanf__, 2, 0))) __attribute__ ((__nonnull__ (1)));
extern int vscanf (const char *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vscanf")
     __attribute__ ((__format__ (__scanf__, 1, 0))) ;
extern int vsscanf (const char *__restrict __s, const char *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vsscanf") __attribute__ ((__nothrow__ , __leaf__))
     __attribute__ ((__format__ (__scanf__, 2, 0)));
extern int fgetc (FILE *__stream) __attribute__ ((__nonnull__ (1)));
extern int getc (FILE *__stream) __attribute__ ((__nonnull__ (1)));
extern int getchar (void);
extern int fputc (int __c, FILE *__stream) __attribute__ ((__nonnull__ (2)));
extern int putc (int __c, FILE *__stream) __attribute__ ((__nonnull__ (2)));
extern int putchar (int __c);
extern char *fgets (char *__restrict __s, int __n, FILE *__restrict __stream)
     __attribute__ ((__access__ (__write_only__, 1, 2))) __attribute__ ((__nonnull__ (3)));
extern int fputs (const char *__restrict __s, FILE *__restrict __stream)
  __attribute__ ((__nonnull__ (2)));
extern int puts (const char *__s);
extern int ungetc (int __c, FILE *__stream) __attribute__ ((__nonnull__ (2)));
extern size_t fread (void *__restrict __ptr, size_t __size,
       size_t __n, FILE *__restrict __stream)
  __attribute__ ((__nonnull__ (4)));
extern size_t fwrite (const void *__restrict __ptr, size_t __size,
        size_t __n, FILE *__restrict __s) __attribute__ ((__nonnull__ (4)));
extern int fseek (FILE *__stream, long int __off, int __whence)
  __attribute__ ((__nonnull__ (1)));
extern long int ftell (FILE *__stream) __attribute__ ((__nonnull__ (1)));
extern void rewind (FILE *__stream) __attribute__ ((__nonnull__ (1)));
extern int fgetpos (FILE *__restrict __stream, fpos_t *__restrict __pos)
  __attribute__ ((__nonnull__ (1)));
extern int fsetpos (FILE *__stream, const fpos_t *__pos) __attribute__ ((__nonnull__ (1)));
extern void clearerr (FILE *__stream) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int feof (FILE *__stream) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int ferror (FILE *__stream) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern void perror (const char *__s) __attribute__ ((__cold__));
extern int __uflow (FILE *);
extern int __overflow (FILE *, int);

typedef long int wchar_t;

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

extern unsigned int __VERIFIER_nondet_uint();
extern int __VERIFIER_nondet_int();
extern unsigned long __VERIFIER_nondet_ulong();
extern long __VERIFIER_nondet_long();
extern unsigned long long __VERIFIER_nondet_ulonglong();
extern long long __VERIFIER_nondet_longlong();
void reach_error() { ((0) ? (void) (0) : __assert_fail ("0", "builtin_popcount32_symbolic.c", 22, __extension__ __PRETTY_FUNCTION__)); }
void __VERIFIER_assert(int cond) {
    if (!(cond)) {
          ERROR: {reach_error();abort();}
                   }
      return;
}
void test_popcount() {
  unsigned int test_int1 = 1231;
  unsigned int test_int2 = 0;
  unsigned int test_int3 = 1;
  unsigned int test_uint16BitMaxValue = 65535;
  int nondet_int = __VERIFIER_nondet_int();
  if (nondet_int != test_int1 && nondet_int != test_int2 && nondet_int != test_int3 && nondet_int != test_uint16BitMaxValue) {
    return;
  }
  int intRes1 = __builtin_popcount(nondet_int) == 7;
  int intRes2 = __builtin_popcount(nondet_int) == 16;
  int intRes3 = __builtin_popcount(nondet_int) == 0;
  int intRes4 = __builtin_popcount(nondet_int) == 1;
  __VERIFIER_assert(intRes1 || intRes2 || intRes3 || intRes4);
  unsigned int nondet_uint = __VERIFIER_nondet_uint();
  if (nondet_uint != test_int1 && nondet_uint != test_int2 && nondet_uint != test_int3 && nondet_uint != test_uint16BitMaxValue) {
    return;
  }
  int uintRes1 = __builtin_popcount(nondet_uint) == 7;
  int uintRes2 = __builtin_popcount(nondet_uint) == 16;
  int uintRes3 = __builtin_popcount(nondet_uint) == 0;
  int uintRes4 = __builtin_popcount(nondet_uint) == 1;
  __VERIFIER_assert(uintRes1 || uintRes2 || uintRes3 || uintRes4);
  int nondet_intMax = __VERIFIER_nondet_int();
  if (!(nondet_intMax > 2147483646)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcount(nondet_intMax) == 31);
  int nondet_intMin = __VERIFIER_nondet_int();
  if (!(nondet_intMin < -2147483647)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcount(nondet_intMin) == 1);
  int nondet_intAtLeastMinPlusOne = __VERIFIER_nondet_int();
  if (!(nondet_intAtLeastMinPlusOne < -2147483646)) {
    return;
  }
  int count_intAtLeastMinPlusOne = __builtin_popcount(nondet_intAtLeastMinPlusOne);
  if (count_intAtLeastMinPlusOne == 1) {
    return;
  }
  __VERIFIER_assert(count_intAtLeastMinPlusOne == 2);
  int nondet_uintMax = __VERIFIER_nondet_uint();
  if (!(nondet_uintMax > 4294967294)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcount(nondet_uintMax) == 32);
  int nondet_uintAtLeastMaxMinusOne = __VERIFIER_nondet_uint();
  if (!(nondet_uintAtLeastMaxMinusOne > 4294967293)) {
    return;
  }
  int count_uintAtLeastMaxMinusOne = __builtin_popcount(nondet_uintAtLeastMaxMinusOne);
  __VERIFIER_assert(count_uintAtLeastMaxMinusOne == 31 || count_uintAtLeastMaxMinusOne == 32);
}
void test_popcountl() {
  unsigned long test_int1 = 1231;
  unsigned long test_int2 = 0;
  unsigned long test_int3 = 1;
  unsigned long test_int4 = 162368;
  unsigned long test_uint16BitMaxValue = 65535;
  long nondet_long = __VERIFIER_nondet_long();
  if (nondet_long != test_int1 && nondet_long != test_int2 && nondet_long != test_int3 && nondet_long != test_int4 && nondet_long != test_uint16BitMaxValue) {
    return;
  }
  int longRes1 = __builtin_popcountl(nondet_long) == 7;
  int longRes2 = __builtin_popcountl(nondet_long) == 0;
  int longRes3 = __builtin_popcountl(nondet_long) == 1;
  int longRes4 = __builtin_popcountl(nondet_long) == 7;
  int longRes5 = __builtin_popcountl(nondet_long) == 16;
  __VERIFIER_assert(longRes1 || longRes2 || longRes3 || longRes4 || longRes5);
  unsigned long nondet_ulong = __VERIFIER_nondet_ulong();
  if (nondet_ulong != test_int1 && nondet_ulong != test_int2 && nondet_ulong != test_int3 && nondet_ulong != test_int4 && nondet_ulong != test_uint16BitMaxValue) {
    return;
  }
  int ulongRes1 = __builtin_popcountl(nondet_ulong) == 7;
  int ulongRes2 = __builtin_popcountl(nondet_ulong) == 0;
  int ulongRes3 = __builtin_popcountl(nondet_ulong) == 1;
  int ulongRes4 = __builtin_popcountl(nondet_ulong) == 7;
  int ulongRes5 = __builtin_popcountl(nondet_ulong) == 16;
  __VERIFIER_assert(ulongRes1 || ulongRes2 || ulongRes3 || ulongRes4 || ulongRes5);
  long nondet_longMax = __VERIFIER_nondet_long();
  if (!(nondet_longMax > 2147483646L)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcountl(nondet_longMax) == 31);
  long nondet_longMin = __VERIFIER_nondet_long();
  if (!(nondet_longMin < -2147483647L)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcountl(nondet_longMin) == 1);
  long nondet_longAtLeastMinPlusOne = __VERIFIER_nondet_long();
  if (!(nondet_longAtLeastMinPlusOne < -2147483646L)) {
    return;
  }
  long count_longAtLeastMinPlusOne = __builtin_popcountl(nondet_longAtLeastMinPlusOne);
  if (count_longAtLeastMinPlusOne == 1) {
    return;
  }
  __VERIFIER_assert(count_longAtLeastMinPlusOne == 2);
  unsigned long nondet_ulongMax = __VERIFIER_nondet_ulong();
  if (!(nondet_ulongMax > 4294967294UL)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcountl(nondet_ulongMax) == 32);
  unsigned long nondet_ulongAtLeastMaxMinusOne = __VERIFIER_nondet_ulong();
  if (!(nondet_ulongAtLeastMaxMinusOne > 4294967293UL)) {
    return;
  }
  int count_ulongAtLeastMaxMinusOne = __builtin_popcountl(nondet_ulongAtLeastMaxMinusOne);
  __VERIFIER_assert(count_ulongAtLeastMaxMinusOne == 31 || count_ulongAtLeastMaxMinusOne == 32);
}
void test_popcountll() {
  unsigned long long test_int1 = 1231;
  unsigned long long test_int2 = 0;
  unsigned long long test_int3 = 1;
  unsigned long long test_int4 = 162368;
  unsigned long long test_uint16BitMaxValue = 65535;
  unsigned long long test_uint32BitMaxValue = 4294967295UL;
  long long nondet_ll = __VERIFIER_nondet_longlong();
  if (nondet_ll != test_int1 && nondet_ll != test_int2 && nondet_ll != test_int3 && nondet_ll != test_int4 && nondet_ll != test_uint16BitMaxValue && nondet_ll != test_uint32BitMaxValue) {
    return;
  }
  int longRes1 = __builtin_popcountll(nondet_ll) == 7;
  int longRes2 = __builtin_popcountll(nondet_ll) == 0;
  int longRes3 = __builtin_popcountll(nondet_ll) == 1;
  int longRes4 = __builtin_popcountll(nondet_ll) == 7;
  int longRes5 = __builtin_popcountll(nondet_ll) == 16;
  int longRes6 = __builtin_popcountll(nondet_ll) == 32;
  __VERIFIER_assert(longRes1 || longRes2 || longRes3 || longRes4 || longRes5 || longRes6);
  unsigned long long nondet_ull = __VERIFIER_nondet_ulonglong();
  if (nondet_ull != test_int1 && nondet_ull != test_int2 && nondet_ull != test_int3 && nondet_ull != test_int4 && nondet_ull != test_uint16BitMaxValue) {
    return;
  }
  int ulongRes1 = __builtin_popcountll(nondet_ull) == 7;
  int ulongRes2 = __builtin_popcountll(nondet_ull) == 0;
  int ulongRes3 = __builtin_popcountll(nondet_ull) == 1;
  int ulongRes4 = __builtin_popcountll(nondet_ull) == 7;
  int ulongRes5 = __builtin_popcountll(nondet_ull) == 16;
  __VERIFIER_assert(ulongRes1 || ulongRes2 || ulongRes3 || ulongRes4 || ulongRes5);
  long long nondet_llMax = __VERIFIER_nondet_longlong();
  if (!(nondet_llMax > 9223372036854775806LL)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcountll(nondet_llMax) == 63);
  long long nondet_llMin = __VERIFIER_nondet_longlong();
  if (!(nondet_llMin < -9223372036854775807LL)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcountll(nondet_llMin) == 1);
  long long nondet_llAtLeastMinPlusOne = __VERIFIER_nondet_longlong();
  if (!(nondet_llAtLeastMinPlusOne < -9223372036854775806LL)) {
    return;
  }
  int count_llAtLeastMinPlusOne = __builtin_popcountll(nondet_llAtLeastMinPlusOne);
  if (count_llAtLeastMinPlusOne == 1) {
    return;
  }
  __VERIFIER_assert(count_llAtLeastMinPlusOne == 2);
  long long nondet_llMin2 = __VERIFIER_nondet_ulonglong();
  if (!(nondet_llMin2 < 0 && nondet_llMin2 >= -2LL)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcountll(nondet_llMin2) == 64 || __builtin_popcountll(nondet_llMin2) == 63);
  unsigned long long nondet_ullMax = __VERIFIER_nondet_ulonglong();
  if (!(nondet_ullMax > 18446744073709551614ULL)) {
    return;
  }
  __VERIFIER_assert(__builtin_popcountll(nondet_ullMax) == 64);
  unsigned long long nondet_ullAtLeastMaxMinusOne = __VERIFIER_nondet_ulonglong();
  if (!(nondet_ullAtLeastMaxMinusOne > 18446744073709551613ULL)) {
    return;
  }
  int count_ullAtLeastMaxMinusOne = __builtin_popcountll(nondet_ullAtLeastMaxMinusOne);
  __VERIFIER_assert(count_ullAtLeastMaxMinusOne == 63 || count_ullAtLeastMaxMinusOne == 64);
}
int main() {
  test_popcount();
  test_popcountl();
  test_popcountll();
  return 0;
}
