// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <limits.h>
#include <stdlib.h>
#include <time.h>

extern int __VERIFIER_nondet_int();
extern unsigned int __VERIFIER_nondet_uint();
extern long __VERIFIER_nondet_long();
extern unsigned long __VERIFIER_nondet_ulong();
extern long long __VERIFIER_nondet_longlong();
extern unsigned long long __VERIFIER_nondet_ulonglong();

// Precomputed sqrt boundaries
#define INT_SQRT_FLOOR       46340
#define UINT_SQRT_FLOOR      65535
#define LONG_SQRT_FLOOR      3037000499L
#define ULONG_SQRT_FLOOR     4294967295UL
#define LL_SQRT_FLOOR        3037000499LL
#define ULL_SQRT_FLOOR       4294967295ULL

void test_mul_int() {

  // Range without overflow
  {
    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();
    int res;

    if (!(a >= INT_SQRT_FLOOR - 10 && a <= INT_SQRT_FLOOR)) return;
    if (!(b >= INT_SQRT_FLOOR - 10 && b <= INT_SQRT_FLOOR)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Range with guaranteed overflow
  {
    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();
    int res;

    if (!(a >= INT_SQRT_FLOOR + 1 && a <= INT_SQRT_FLOOR + 10)) return;
    if (!(b >= INT_SQRT_FLOOR + 1 && b <= INT_SQRT_FLOOR + 10)) return;
    if (!__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Mixed boundary range
  {
    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();
    int res;

    if (!(a >= INT_SQRT_FLOOR - 5 && a <= INT_SQRT_FLOOR + 5)) return;
    if (!(b >= INT_SQRT_FLOOR - 5 && b <= INT_SQRT_FLOOR + 5)) return;
    long long exact = (long long)a * (long long)b;
    int ov = (exact > INT_MAX || exact < INT_MIN);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov);
    } else {
      assert(!ov);
      assert(res == (int)exact);
    }
  }

  // Multiplication by zero
  {
    int a = __VERIFIER_nondet_int();
    int b = 0;
    int res;

    if (!(a >= INT_MIN + 100 && a <= INT_MAX - 100)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == 0);
  }

  // Multiplication by +1
  {
    int a = __VERIFIER_nondet_int();
    int b = 1;
    int res;

    if (!(a >= INT_MIN + 100 && a <= INT_MAX - 100)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == a);
  }

  // Multiplication by -1 excluding INT_MIN
  {
    int a = __VERIFIER_nondet_int();
    int b = -1;
    int res;

    if (!(a >= INT_MIN + 1 && a <= INT_MAX)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == -a);
  }

  // INT_MIN * -1 (guaranteed overflow)
  {
    int a = INT_MIN;
    int res;
    if (!__builtin_mul_overflow(a, -1, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // High values still fitting
  {
    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();
    int res;

    if (!(a >= 30000 && a <= 33000)) return;
    if (!(b >= 30000 && b <= 33000)) return;
    long long exact2 = (long long)a * (long long)b;
    int ov2 = (exact2 > INT_MAX || exact2 < INT_MIN);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov2);
    } else {
      assert(!ov2);
      assert(res == (int)exact2);
    }
  }

  // Opposite signs near boundary
  {
    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();
    int res;

    if (!(a >= INT_SQRT_FLOOR - 5 && a <= INT_SQRT_FLOOR + 5)) return;
    if (!(b >= -(INT_SQRT_FLOOR + 5) && b <= -(INT_SQRT_FLOOR - 5))) return;
    long long exact3 = (long long)a * (long long)b;
    int ov3 = (exact3 > INT_MAX || exact3 < INT_MIN);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov3);
    } else {
      assert(!ov3);
      assert(res == (int)exact3);
    }
  }

  // INT_MAX * small positive
  {
    int a = INT_MAX;
    int b = __VERIFIER_nondet_int();
    int res;

    if (!(b >= 0 && b <= 2)) return;
    long long exact4 = (long long)a * (long long)b;
    int ov4 = (exact4 > INT_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov4);
    } else {
      assert(!ov4);
      assert(res == (int)exact4);
    }
  }

  // INT_MIN * small negative
  {
    int a = INT_MIN;
    int b = __VERIFIER_nondet_int();
    int res;

    if (!(b >= -2 && b <= -1)) return;
    if (!__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

ERROR:
  return;
}

void test_mul_uint() {

  // Range without overflow
  {
    unsigned int a = __VERIFIER_nondet_uint();
    unsigned int b = __VERIFIER_nondet_uint();
    unsigned int res;

    if (!(a <= UINT_SQRT_FLOOR && a >= UINT_SQRT_FLOOR - 10)) return;
    if (!(b <= UINT_SQRT_FLOOR && b >= UINT_SQRT_FLOOR - 10)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Range with guaranteed overflow
  {
    unsigned int a = __VERIFIER_nondet_uint();
    unsigned int b = __VERIFIER_nondet_uint();
    unsigned int res;

    if (!(a >= UINT_SQRT_FLOOR + 1 && a <= UINT_SQRT_FLOOR + 20)) return;
    if (!(b >= UINT_SQRT_FLOOR + 1 && b <= UINT_SQRT_FLOOR + 20)) return;
    if (!__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Mixed boundary range
  {
    unsigned int a = __VERIFIER_nondet_uint();
    unsigned int b = __VERIFIER_nondet_uint();
    unsigned int res;

    if (!(a >= UINT_SQRT_FLOOR - 5 && a <= UINT_SQRT_FLOOR + 5)) return;
    if (!(b >= UINT_SQRT_FLOOR - 5 && b <= UINT_SQRT_FLOOR + 5)) return;
    unsigned long long exact = (unsigned long long)a * (unsigned long long)b;
    int ov = (exact > UINT_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov);
    } else {
      assert(!ov);
      assert(res == (unsigned int)exact);
    }
  }

  // Multiplication by zero
  {
    unsigned int a = __VERIFIER_nondet_uint();
    unsigned int b = 0u;
    unsigned int res;

    if (!(a <= UINT_MAX - 100u)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == 0u);
  }

  // Multiplication by +1
  {
    unsigned int a = __VERIFIER_nondet_uint();
    unsigned int b = 1u;
    unsigned int res;

    if (!(a <= UINT_MAX - 100u)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == a);
  }

  // High values still fitting
  {
    unsigned int a = __VERIFIER_nondet_uint();
    unsigned int b = __VERIFIER_nondet_uint();
    unsigned int res;

    if (!(a >= 60000u && a <= 65535u)) return;
    if (!(b >= 60000u && b <= 65535u)) return;
    unsigned long long ex2 = (unsigned long long)a * (unsigned long long)b;
    int ov2 = (ex2 > UINT_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov2);
    } else {
      assert(!ov2);
      assert(res == (unsigned int)ex2);
    }
  }

  // UINT_MAX * small positive
  {
    unsigned int a = UINT_MAX;
    unsigned int b = __VERIFIER_nondet_uint();
    unsigned int res;

    if (!(b <= 2u)) return;
    unsigned long long ex3 = (unsigned long long)a * (unsigned long long)b;
    int ov3 = (ex3 > UINT_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov3);
    } else {
      assert(!ov3);
      assert(res == (unsigned int)ex3);
    }
  }

ERROR:
  return;
}

void test_mul_long() {

  // Range without overflow
  {
    long a = __VERIFIER_nondet_long();
    long b = __VERIFIER_nondet_long();
    long res;

    if (!(a >= LONG_SQRT_FLOOR - 10 && a <= LONG_SQRT_FLOOR)) return;
    if (!(b >= LONG_SQRT_FLOOR - 10 && b <= LONG_SQRT_FLOOR)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Range with guaranteed overflow
  {
    long a = __VERIFIER_nondet_long();
    long b = __VERIFIER_nondet_long();
    long res;

    if (!(a >= LONG_SQRT_FLOOR + 1 && a <= LONG_SQRT_FLOOR + 20)) return;
    if (!(b >= LONG_SQRT_FLOOR + 1 && b <= LONG_SQRT_FLOOR + 20)) return;
    if (!__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Mixed boundary range
  {
    long a = __VERIFIER_nondet_long();
    long b = __VERIFIER_nondet_long();
    long res;

    if (!(a >= LONG_SQRT_FLOOR - 5 && a <= LONG_SQRT_FLOOR + 5)) return;
    if (!(b >= LONG_SQRT_FLOOR - 5 && b <= LONG_SQRT_FLOOR + 5)) return;
    long long exact = (long long)a * (long long)b;
    int ov = (exact > LONG_MAX || exact < LONG_MIN);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov);
    } else {
      assert(!ov);
      assert(res == (long)exact);
    }
  }

  // Multiplication by zero
  {
    long a = __VERIFIER_nondet_long();
    long b = 0L;
    long res;

    if (!(a >= LONG_MIN + 100L && a <= LONG_MAX - 100L)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == 0L);
  }

  // Multiplication by +1
  {
    long a = __VERIFIER_nondet_long();
    long b = 1L;
    long res;

    if (!(a >= LONG_MIN + 100L && a <= LONG_MAX - 100L)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == a);
  }

  // Multiplication by -1 excluding LONG_MIN
  {
    long a = __VERIFIER_nondet_long();
    long b = -1L;
    long res;

    if (!(a >= LONG_MIN + 1L && a <= LONG_MAX)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == -a);
  }

  // LONG_MIN * -1 (overflow)
  {
    long a = LONG_MIN;
    long res;
    if (!__builtin_mul_overflow(a, -1L, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // High values still fitting
  {
    long a = __VERIFIER_nondet_long();
    long b = __VERIFIER_nondet_long();
    long res;

    if (!(a >= LONG_SQRT_FLOOR - 100L && a <= LONG_SQRT_FLOOR)) return;
    if (!(b >= LONG_SQRT_FLOOR - 100L && b <= LONG_SQRT_FLOOR)) return;
    long long ex2 = (long long)a * (long long)b;
    int ov2 = (ex2 > LONG_MAX || ex2 < LONG_MIN);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov2);
    } else {
      assert(!ov2);
      assert(res == (long)ex2);
    }
  }

  // Opposite signs near boundary
  {
    long a = __VERIFIER_nondet_long();
    long b = __VERIFIER_nondet_long();
    long res;

    if (!(a >= LONG_SQRT_FLOOR - 5 && a <= LONG_SQRT_FLOOR + 5)) return;
    if (!(b >= -(LONG_SQRT_FLOOR + 5) && b <= -(LONG_SQRT_FLOOR - 5))) return;
    long long ex3 = (long long)a * (long long)b;
    int ov3 = (ex3 > LONG_MAX || ex3 < LONG_MIN);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov3);
    } else {
      assert(!ov3);
      assert(res == (long)ex3);
    }
  }

  // LONG_MAX * small positive
  {
    long a = LONG_MAX;
    long b = __VERIFIER_nondet_long();
    long res;

    if (!(b >= 0 && b <= 2)) return;
    long long ex4 = (long long)a * (long long)b;
    int ov4 = (ex4 > LONG_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov4);
    } else {
      assert(!ov4);
      assert(res == (long)ex4);
    }
  }

  // LONG_MIN * small negative
  {
    long a = LONG_MIN;
    long b = __VERIFIER_nondet_long();
    long res;

    if (!(b >= -2 && b <= -1)) return;
    if (!__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

ERROR:
  return;
}

void test_mul_ulong() {

  // Range without overflow
  {
    unsigned long a = __VERIFIER_nondet_ulong();
    unsigned long b = __VERIFIER_nondet_ulong();
    unsigned long res;

    if (!(a >= ULONG_SQRT_FLOOR - 10 && a <= ULONG_SQRT_FLOOR)) return;
    if (!(b >= ULONG_SQRT_FLOOR - 10 && b <= ULONG_SQRT_FLOOR)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Range with guaranteed overflow
  {
    unsigned long a = __VERIFIER_nondet_ulong();
    unsigned long b = __VERIFIER_nondet_ulong();
    unsigned long res;

    if (!(a >= ULONG_SQRT_FLOOR + 1 && a <= ULONG_SQRT_FLOOR + 20)) return;
    if (!(b >= ULONG_SQRT_FLOOR + 1 && b <= ULONG_SQRT_FLOOR + 20)) return;
    if (!__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Mixed boundary range
  {
    unsigned long a = __VERIFIER_nondet_ulong();
    unsigned long b = __VERIFIER_nondet_ulong();
    unsigned long res;

    if (!(a >= ULONG_SQRT_FLOOR - 5 && a <= ULONG_SQRT_FLOOR + 5)) return;
    if (!(b >= ULONG_SQRT_FLOOR - 5 && b <= ULONG_SQRT_FLOOR + 5)) return;
    unsigned long long exact = (unsigned long long)a * (unsigned long long)b;
    int ov = (exact > ULONG_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov);
    } else {
      assert(!ov);
      assert(res == (unsigned long)exact);
    }
  }

  // Multiplication by zero
  {
    unsigned long a = __VERIFIER_nondet_ulong();
    unsigned long b = 0UL;
    unsigned long res;

    if (!(a <= ULONG_MAX - 100UL)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == 0UL);
  }

  // Multiplication by +1
  {
    unsigned long a = __VERIFIER_nondet_ulong();
    unsigned long b = 1UL;
    unsigned long res;

    if (!(a <= ULONG_MAX - 100UL)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == a);
  }

  // High values still fitting
  {
    unsigned long a = __VERIFIER_nondet_ulong();
    unsigned long b = __VERIFIER_nondet_ulong();
    unsigned long res;

    if (!(a >= ULONG_SQRT_FLOOR - 50 && a <= ULONG_SQRT_FLOOR)) return;
    if (!(b >= ULONG_SQRT_FLOOR - 50 && b <= ULONG_SQRT_FLOOR)) return;
    unsigned long long ex2 = (unsigned long long)a * (unsigned long long)b;
    int ov2 = (ex2 > ULONG_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov2);
    } else {
      assert(!ov2);
      assert(res == (unsigned long)ex2);
    }
  }

  // ULONG_MAX * small positive
  {
    unsigned long a = ULONG_MAX;
    unsigned long b = __VERIFIER_nondet_ulong();
    unsigned long res;

    if (!(b <= 2UL)) return;
    unsigned long long ex3 = (unsigned long long)a * (unsigned long long)b;
    int ov3 = (ex3 > ULONG_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov3);
    } else {
      assert(!ov3);
      assert(res == (unsigned long)ex3);
    }
  }

ERROR:
  return;
}

void test_mul_longlong() {

  // Range without overflow
  {
    long long a = __VERIFIER_nondet_longlong();
    long long b = __VERIFIER_nondet_longlong();
    long long res;

    if (!(a >= LL_SQRT_FLOOR - 10 && a <= LL_SQRT_FLOOR)) return;
    if (!(b >= LL_SQRT_FLOOR - 10 && b <= LL_SQRT_FLOOR)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Range with guaranteed overflow
  {
    long long a = __VERIFIER_nondet_longlong();
    long long b = __VERIFIER_nondet_longlong();
    long long res;

    if (!(a >= LL_SQRT_FLOOR + 1 && a <= LL_SQRT_FLOOR + 20)) return;
    if (!(b >= LL_SQRT_FLOOR + 1 && b <= LL_SQRT_FLOOR + 20)) return;
    if (!__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Mixed boundary range
  {
    long long a = __VERIFIER_nondet_longlong();
    long long b = __VERIFIER_nondet_longlong();
    long long res;

    if (!(a >= LL_SQRT_FLOOR - 5 && a <= LL_SQRT_FLOOR + 5)) return;
    if (!(b >= LL_SQRT_FLOOR - 5 && b <= LL_SQRT_FLOOR + 5)) return;
    __int128 exact = (__int128)a * (__int128)b;
    int ov = (exact > LLONG_MAX || exact < LLONG_MIN);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov);
    } else {
      assert(!ov);
      assert(res == (long long)exact);
    }
  }

  // Multiplication by zero
  {
    long long a = __VERIFIER_nondet_longlong();
    long long b = 0LL;
    long long res;

    if (!(a >= LLONG_MIN + 100LL && a <= LLONG_MAX - 100LL)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == 0LL);
  }

  // Multiplication by +1
  {
    long long a = __VERIFIER_nondet_longlong();
    long long b = 1LL;
    long long res;

    if (!(a >= LLONG_MIN + 100LL && a <= LLONG_MAX - 100LL)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == a);
  }

  // Multiplication by -1 excluding LLONG_MIN
  {
    long long a = __VERIFIER_nondet_longlong();
    long long b = -1LL;
    long long res;

    if (!(a >= LLONG_MIN + 1LL && a <= LLONG_MAX)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == -a);
  }

  // LLONG_MIN * -1 (overflow)
  {
    long long a = LLONG_MIN;
    long long res;
    if (!__builtin_mul_overflow(a, -1LL, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // High values still fitting
  {
    long long a = __VERIFIER_nondet_longlong();
    long long b = __VERIFIER_nondet_longlong();
    long long res;

    if (!(a >= LL_SQRT_FLOOR - 50 && a <= LL_SQRT_FLOOR)) return;
    if (!(b >= LL_SQRT_FLOOR - 50 && b <= LL_SQRT_FLOOR)) return;
    __int128 ex2 = (__int128)a * (__int128)b;
    int ov2 = (ex2 > LLONG_MAX || ex2 < LLONG_MIN);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov2);
    } else {
      assert(!ov2);
      assert(res == (long long)ex2);
    }
  }

  // Opposite signs near boundary
  {
    long long a = __VERIFIER_nondet_longlong();
    long long b = __VERIFIER_nondet_longlong();
    long long res;

    if (!(a >= LL_SQRT_FLOOR - 5 && a <= LL_SQRT_FLOOR + 5)) return;
    if (!(b >= -(LL_SQRT_FLOOR + 5) && b <= -(LL_SQRT_FLOOR - 5))) return;
    __int128 ex3 = (__int128)a * (__int128)b;
    int ov3 = (ex3 > LLONG_MAX || ex3 < LLONG_MIN);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov3);
    } else {
      assert(!ov3);
      assert(res == (long long)ex3);
    }
  }

  // LLONG_MAX * small positive
  {
    long long a = LLONG_MAX;
    long long b = __VERIFIER_nondet_longlong();
    long long res;

    if (!(b >= 0 && b <= 2)) return;
    __int128 ex4 = (__int128)a * (__int128)b;
    int ov4 = (ex4 > LLONG_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov4);
    } else {
      assert(!ov4);
      assert(res == (long long)ex4);
    }
  }

  // LLONG_MIN * small negative
  {
    long long a = LLONG_MIN;
    long long b = __VERIFIER_nondet_longlong();
    long long res;

    if (!(b >= -2 && b <= -1)) return;
    if (!__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

ERROR:
  return;
}

void test_mul_ulonglong() {

  // Range without overflow
  {
    unsigned long long a = __VERIFIER_nondet_ulonglong();
    unsigned long long b = __VERIFIER_nondet_ulonglong();
    unsigned long long res;

    if (!(a >= ULL_SQRT_FLOOR - 10 && a <= ULL_SQRT_FLOOR)) return;
    if (!(b >= ULL_SQRT_FLOOR - 10 && b <= ULL_SQRT_FLOOR)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Range with guaranteed overflow
  {
    unsigned long long a = __VERIFIER_nondet_ulonglong();
    unsigned long long b = __VERIFIER_nondet_ulonglong();
    unsigned long long res;

    if (!(a >= ULL_SQRT_FLOOR + 1 && a <= ULL_SQRT_FLOOR + 20)) return;
    if (!(b >= ULL_SQRT_FLOOR + 1 && b <= ULL_SQRT_FLOOR + 20)) return;
    if (!__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
  }

  // Mixed boundary range
  {
    unsigned long long a = __VERIFIER_nondet_ulonglong();
    unsigned long long b = __VERIFIER_nondet_ulonglong();
    unsigned long long res;

    if (!(a >= ULL_SQRT_FLOOR - 5 && a <= ULL_SQRT_FLOOR + 5)) return;
    if (!(b >= ULL_SQRT_FLOOR - 5 && b <= ULL_SQRT_FLOOR + 5)) return;
    __uint128_t exact = (__uint128_t)a * (__uint128_t)b;
    int ov = (exact > ULLONG_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov);
    } else {
      assert(!ov);
      assert(res == (unsigned long long)exact);
    }
  }

  // Multiplication by zero
  {
    unsigned long long a = __VERIFIER_nondet_ulonglong();
    unsigned long long b = 0ULL;
    unsigned long long res;

    if (!(a <= ULLONG_MAX - 100ULL)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == 0ULL);
  }

  // Multiplication by +1
  {
    unsigned long long a = __VERIFIER_nondet_ulonglong();
    unsigned long long b = 1ULL;
    unsigned long long res;

    if (!(a <= ULLONG_MAX - 100ULL)) return;
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(0);
      goto ERROR;
    }
    assert(res == a);
  }

  // ULLONG_MAX * small positive
  {
    unsigned long long a = ULLONG_MAX;
    unsigned long long b = __VERIFIER_nondet_ulonglong();
    unsigned long long res;

    if (!(b <= 2ULL)) return;
    __uint128_t ex2 = (__uint128_t)a * (__uint128_t)b;
    int ov2 = (ex2 > ULLONG_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov2);
    } else {
      assert(!ov2);
      assert(res == (unsigned long long)ex2);
    }
  }

  // High values still fitting
  {
    unsigned long long a = __VERIFIER_nondet_ulonglong();
    unsigned long long b = __VERIFIER_nondet_ulonglong();
    unsigned long long res;

    if (!(a >= ULL_SQRT_FLOOR - 50 && a <= ULL_SQRT_FLOOR)) return;
    if (!(b >= ULL_SQRT_FLOOR - 50 && b <= ULL_SQRT_FLOOR)) return;
    __uint128_t ex3 = (__uint128_t)a * (__uint128_t)b;
    int ov3 = (ex3 > ULLONG_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov3);
    } else {
      assert(!ov3);
      assert(res == (unsigned long long)ex3);
    }
  }

  // Cross-range min/max checks
  {
    unsigned long long a = __VERIFIER_nondet_ulonglong();
    unsigned long long b = __VERIFIER_nondet_ulonglong();
    unsigned long long res;

    if (!(a >= ULL_SQRT_FLOOR - 3 && a <= ULL_SQRT_FLOOR + 3)) return;
    if (!(b >= 1ULL && b <= 3ULL)) return;
    __uint128_t ex4 = (__uint128_t)a * (__uint128_t)b;
    int ov4 = (ex4 > ULLONG_MAX);
    if (__builtin_mul_overflow(a, b, &res)) {
      assert(ov4);
    } else {
      assert(!ov4);
      assert(res == (unsigned long long)ex4);
    }
  }

ERROR:
  return;
}

int main() {
  test_mul_int();
  test_mul_uint();
  test_mul_long();
  test_mul_ulong();
  test_mul_longlong();
  test_mul_ulonglong();
  return 0;
}