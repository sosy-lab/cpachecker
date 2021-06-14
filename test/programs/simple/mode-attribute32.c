// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>

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

  assert(sizeof(test_int8_t) == 1);
  assert(sizeof(test_int16_t) == 2);
  assert(sizeof(test_int32_t) == 4);
  assert(sizeof(test_int64_t) == 8);
  assert(sizeof(test_u_int8_t) == 1);
  assert(sizeof(test_u_int16_t) == 2);
  assert(sizeof(test_u_int32_t) == 4);
  assert(sizeof(test_u_int64_t) == 8);

  assert(sizeof(test_int_word_t) == 4);

  // Check that unsignedness is correctly applied
  test_u_int8_t c = 255;
  assert(((int)c) == 255);
}
