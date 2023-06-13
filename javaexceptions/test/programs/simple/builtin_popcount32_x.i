// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
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

extern int __builtin_popcountll (unsigned long long);
extern int __builtin_popcountl (unsigned long);
extern int __builtin_popcount (unsigned int x);
int main() {
  test_popcount();
  test_popcountl();
  test_popcountll();
  return 0;
}
void test_popcount(){
  unsigned int test_int1 = 1231;
  unsigned int test_int2 = 0;
  unsigned int test_int3 = 1;
  unsigned int test_uint16BitMaxValue = 65535;
  ((void) sizeof ((__builtin_popcount(test_int1) == 7) ? 1 : 0), __extension__ ({ if (__builtin_popcount(test_int1) == 7) ; else __assert_fail ("__builtin_popcount(test_int1) == 7", "builtin_popcount32_x.c", 29, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcount(test_int2) == 0) ? 1 : 0), __extension__ ({ if (__builtin_popcount(test_int2) == 0) ; else __assert_fail ("__builtin_popcount(test_int2) == 0", "builtin_popcount32_x.c", 30, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcount(test_int3) == 1) ? 1 : 0), __extension__ ({ if (__builtin_popcount(test_int3) == 1) ; else __assert_fail ("__builtin_popcount(test_int3) == 1", "builtin_popcount32_x.c", 31, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcount(test_uint16BitMaxValue) == 16) ? 1 : 0), __extension__ ({ if (__builtin_popcount(test_uint16BitMaxValue) == 16) ; else __assert_fail ("__builtin_popcount(test_uint16BitMaxValue) == 16", "builtin_popcount32_x.c", 32, __extension__ __PRETTY_FUNCTION__); }));
}
void test_popcountl(){
  unsigned long test_int1 = 1231;
  unsigned long test_int2 = 0;
  unsigned long test_int3 = 1;
  unsigned long test_int4 = 162368;
  unsigned long test_uint16BitMaxValue = 65535;
  ((void) sizeof ((__builtin_popcountl(test_int1) == 7) ? 1 : 0), __extension__ ({ if (__builtin_popcountl(test_int1) == 7) ; else __assert_fail ("__builtin_popcountl(test_int1) == 7", "builtin_popcount32_x.c", 44, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcountl(test_int2) == 0) ? 1 : 0), __extension__ ({ if (__builtin_popcountl(test_int2) == 0) ; else __assert_fail ("__builtin_popcountl(test_int2) == 0", "builtin_popcount32_x.c", 45, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcountl(test_int3) == 1) ? 1 : 0), __extension__ ({ if (__builtin_popcountl(test_int3) == 1) ; else __assert_fail ("__builtin_popcountl(test_int3) == 1", "builtin_popcount32_x.c", 46, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcountl(test_int4) == 7) ? 1 : 0), __extension__ ({ if (__builtin_popcountl(test_int4) == 7) ; else __assert_fail ("__builtin_popcountl(test_int4) == 7", "builtin_popcount32_x.c", 47, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcountl(test_uint16BitMaxValue) == 16) ? 1 : 0), __extension__ ({ if (__builtin_popcountl(test_uint16BitMaxValue) == 16) ; else __assert_fail ("__builtin_popcountl(test_uint16BitMaxValue) == 16", "builtin_popcount32_x.c", 48, __extension__ __PRETTY_FUNCTION__); }));
}
void test_popcountll(){
  unsigned long long test_int1 = 1231;
  unsigned long long test_int2 = 0;
  unsigned long long test_int3 = 1;
  unsigned long long test_int4 = 162368;
  unsigned long long test_uint16BitMaxValue = 65535;
  unsigned long long test_uint32BitMaxValue = 4294967295UL;
  ((void) sizeof ((__builtin_popcountll(test_int1) == 7) ? 1 : 0), __extension__ ({ if (__builtin_popcountll(test_int1) == 7) ; else __assert_fail ("__builtin_popcountll(test_int1) == 7", "builtin_popcount32_x.c", 63, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcountll(test_int2) == 0) ? 1 : 0), __extension__ ({ if (__builtin_popcountll(test_int2) == 0) ; else __assert_fail ("__builtin_popcountll(test_int2) == 0", "builtin_popcount32_x.c", 64, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcountll(test_int3) == 1) ? 1 : 0), __extension__ ({ if (__builtin_popcountll(test_int3) == 1) ; else __assert_fail ("__builtin_popcountll(test_int3) == 1", "builtin_popcount32_x.c", 65, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcountll(test_int4) == 7) ? 1 : 0), __extension__ ({ if (__builtin_popcountll(test_int4) == 7) ; else __assert_fail ("__builtin_popcountll(test_int4) == 7", "builtin_popcount32_x.c", 66, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcountll(test_uint16BitMaxValue) == 16) ? 1 : 0), __extension__ ({ if (__builtin_popcountll(test_uint16BitMaxValue) == 16) ; else __assert_fail ("__builtin_popcountll(test_uint16BitMaxValue) == 16", "builtin_popcount32_x.c", 68, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((__builtin_popcountll(test_uint32BitMaxValue) == 32) ? 1 : 0), __extension__ ({ if (__builtin_popcountll(test_uint32BitMaxValue) == 32) ; else __assert_fail ("__builtin_popcountll(test_uint32BitMaxValue) == 32", "builtin_popcount32_x.c", 69, __extension__ __PRETTY_FUNCTION__); }));
}
