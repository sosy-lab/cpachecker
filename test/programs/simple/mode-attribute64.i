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

typedef int test_int8_t __attribute__ ((__mode__ (__QI__)));
typedef int test_int16_t __attribute__ ((__mode__ (__HI__)));
typedef int test_int32_t __attribute__ ((__mode__ (__SI__)));
typedef int test_int64_t __attribute__ ((__mode__ (__DI__)));
typedef int test_int_word_t __attribute__ ((__mode__ (__word__)));
typedef unsigned int test_u_int8_t __attribute__ ((__mode__ (__QI__)));
typedef unsigned int test_u_int16_t __attribute__ ((__mode__ (__HI__)));
typedef unsigned int test_u_int32_t __attribute__ ((__mode__ (__SI__)));
typedef unsigned int test_u_int64_t __attribute__ ((__mode__ (__DI__)));
void main() {
  ((void) sizeof ((sizeof(test_int8_t) == 1) ? 1 : 0), __extension__ ({ if (sizeof(test_int8_t) == 1) ; else __assert_fail ("sizeof(test_int8_t) == 1", "mode-attribute64.c", 15, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((sizeof(test_int16_t) == 2) ? 1 : 0), __extension__ ({ if (sizeof(test_int16_t) == 2) ; else __assert_fail ("sizeof(test_int16_t) == 2", "mode-attribute64.c", 16, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((sizeof(test_int32_t) == 4) ? 1 : 0), __extension__ ({ if (sizeof(test_int32_t) == 4) ; else __assert_fail ("sizeof(test_int32_t) == 4", "mode-attribute64.c", 17, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((sizeof(test_int64_t) == 8) ? 1 : 0), __extension__ ({ if (sizeof(test_int64_t) == 8) ; else __assert_fail ("sizeof(test_int64_t) == 8", "mode-attribute64.c", 18, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((sizeof(test_u_int8_t) == 1) ? 1 : 0), __extension__ ({ if (sizeof(test_u_int8_t) == 1) ; else __assert_fail ("sizeof(test_u_int8_t) == 1", "mode-attribute64.c", 19, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((sizeof(test_u_int16_t) == 2) ? 1 : 0), __extension__ ({ if (sizeof(test_u_int16_t) == 2) ; else __assert_fail ("sizeof(test_u_int16_t) == 2", "mode-attribute64.c", 20, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((sizeof(test_u_int32_t) == 4) ? 1 : 0), __extension__ ({ if (sizeof(test_u_int32_t) == 4) ; else __assert_fail ("sizeof(test_u_int32_t) == 4", "mode-attribute64.c", 21, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((sizeof(test_u_int64_t) == 8) ? 1 : 0), __extension__ ({ if (sizeof(test_u_int64_t) == 8) ; else __assert_fail ("sizeof(test_u_int64_t) == 8", "mode-attribute64.c", 22, __extension__ __PRETTY_FUNCTION__); }));
  ((void) sizeof ((sizeof(test_int_word_t) == 8) ? 1 : 0), __extension__ ({ if (sizeof(test_int_word_t) == 8) ; else __assert_fail ("sizeof(test_int_word_t) == 8", "mode-attribute64.c", 24, __extension__ __PRETTY_FUNCTION__); }));
  test_u_int8_t c = 255;
  ((void) sizeof ((((int)c) == 255) ? 1 : 0), __extension__ ({ if (((int)c) == 255) ; else __assert_fail ("((int)c) == 255", "mode-attribute64.c", 28, __extension__ __PRETTY_FUNCTION__); }));
}
