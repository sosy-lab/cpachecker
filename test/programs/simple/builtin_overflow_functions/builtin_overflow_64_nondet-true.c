// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <limits.h>

extern int __VERIFIER_nondet_int();
extern unsigned int __VERIFIER_nondet_uint();

extern long __VERIFIER_nondet_long();
extern unsigned long __VERIFIER_nondet_ulong();

extern long long __VERIFIER_nondet_longlong();
extern unsigned long long __VERIFIER_nondet_ulonglong();


// Precomputed floor(sqrt(x)) for each type
#define INT_SQRT_FLOOR       46340            // floor(sqrt(2^31-1))
#define UINT_SQRT_FLOOR      65535            // floor(sqrt(2^32-1))

#define LONG_SQRT_FLOOR      3037000499L      // floor(sqrt(2^63-1))
#define ULONG_SQRT_FLOOR     4294967295UL     // floor(sqrt(2^64-1))

#define LL_SQRT_FLOOR        3037000499LL
#define ULL_SQRT_FLOOR       4294967295ULL

void test_mul_int() {
  int a = __VERIFIER_nondet_int();
  int b = __VERIFIER_nondet_int();
  int res;

  // Range without overflow
  if (!(a >= INT_SQRT_FLOOR - 10 && a <= INT_SQRT_FLOOR)) return;
  if (!(b >= INT_SQRT_FLOOR - 10 && b <= INT_SQRT_FLOOR)) return;
  if (__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Range with guaranteed overflow
  a = __VERIFIER_nondet_int();
  b = __VERIFIER_nondet_int();
  if (!(a >= INT_SQRT_FLOOR + 1 && a <= INT_SQRT_FLOOR + 10)) return;
  if (!(b >= INT_SQRT_FLOOR + 1 && b <= INT_SQRT_FLOOR + 10)) return;
  if (!__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Mixed boundary range
  a = __VERIFIER_nondet_int();
  b = __VERIFIER_nondet_int();
  if (!(a >= INT_SQRT_FLOOR - 5 && a <= INT_SQRT_FLOOR + 5)) return;
  if (!(b >= INT_SQRT_FLOOR - 5 && b <= INT_SQRT_FLOOR + 5)) return;
  long long exact = (long long)a*(long long)b;
  int ov = (exact > INT_MAX || exact < INT_MIN);
  if (__builtin_mul_overflow(a,b,&res)) {
    assert(ov);
  } else {
    assert(!ov);
    assert(res == (int)exact);
  }
}

void test_mul_uint() {
  unsigned int a = __VERIFIER_nondet_uint();
  unsigned int b = __VERIFIER_nondet_uint();
  unsigned int res;

  // Range without overflow
  if (!(a <= UINT_SQRT_FLOOR && a >= UINT_SQRT_FLOOR - 10)) return;
  if (!(b <= UINT_SQRT_FLOOR && b >= UINT_SQRT_FLOOR - 10)) return;
  if (__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Range with guaranteed overflow
  a = __VERIFIER_nondet_uint();
  b = __VERIFIER_nondet_uint();
  if (!(a >= UINT_SQRT_FLOOR + 1 && a <= UINT_SQRT_FLOOR + 20)) return;
  if (!(b >= UINT_SQRT_FLOOR + 1 && b <= UINT_SQRT_FLOOR + 20)) return;
  if (!__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Mixed boundary range
  a = __VERIFIER_nondet_uint();
  b = __VERIFIER_nondet_uint();
  if (!(a >= UINT_SQRT_FLOOR - 5 && a <= UINT_SQRT_FLOOR + 5)) return;
  if (!(b >= UINT_SQRT_FLOOR - 5 && b <= UINT_SQRT_FLOOR + 5)) return;
  unsigned long long exact = (unsigned long long)a * (unsigned long long)b;
  int ov = (exact > UINT_MAX);
  if (__builtin_mul_overflow(a,b,&res)) {
    assert(ov);
  } else {
    assert(!ov);
    assert(res == (unsigned int)exact);
  }
}

void test_mul_long() {
  long a = __VERIFIER_nondet_long();
  long b = __VERIFIER_nondet_long();
  long res;

  // Range without overflow
  if (!(a >= LONG_SQRT_FLOOR - 10 && a <= LONG_SQRT_FLOOR)) return;
  if (!(b >= LONG_SQRT_FLOOR - 10 && b <= LONG_SQRT_FLOOR)) return;
  if (__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Range with guaranteed overflow
  a = __VERIFIER_nondet_long();
  b = __VERIFIER_nondet_long();
  if (!(a >= LONG_SQRT_FLOOR + 1 && a <= LONG_SQRT_FLOOR + 20)) return;
  if (!(b >= LONG_SQRT_FLOOR + 1 && b <= LONG_SQRT_FLOOR + 20)) return;
  if (!__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Mixed boundary range
  a = __VERIFIER_nondet_long();
  b = __VERIFIER_nondet_long();
  if (!(a >= LONG_SQRT_FLOOR - 5 && a <= LONG_SQRT_FLOOR + 5)) return;
  if (!(b >= LONG_SQRT_FLOOR - 5 && b <= LONG_SQRT_FLOOR + 5)) return;
  long long exact = (long long)a*(long long)b;
  int ov = (exact > LONG_MAX || exact < LONG_MIN);
  if (__builtin_mul_overflow(a,b,&res)) {
    assert(ov);
  } else {
    assert(!ov);
    assert(res == (long)exact);
  }
}

void test_mul_ulong() {
  unsigned long a = __VERIFIER_nondet_ulong();
  unsigned long b = __VERIFIER_nondet_ulong();
  unsigned long res;

  // Range without overflow
  if (!(a >= ULONG_SQRT_FLOOR - 10 && a <= ULONG_SQRT_FLOOR)) return;
  if (!(b >= ULONG_SQRT_FLOOR - 10 && b <= ULONG_SQRT_FLOOR)) return;
  if (__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Range with guaranteed overflow
  a = __VERIFIER_nondet_ulong();
  b = __VERIFIER_nondet_ulong();
  if (!(a >= ULONG_SQRT_FLOOR + 1 && a <= ULONG_SQRT_FLOOR + 20)) return;
  if (!(b >= ULONG_SQRT_FLOOR + 1 && b <= ULONG_SQRT_FLOOR + 20)) return;
  if (!__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Mixed boundary range
  a = __VERIFIER_nondet_ulong();
  b = __VERIFIER_nondet_ulong();
  if (!(a >= ULONG_SQRT_FLOOR - 5 && a <= ULONG_SQRT_FLOOR + 5)) return;
  if (!(b >= ULONG_SQRT_FLOOR - 5 && b <= ULONG_SQRT_FLOOR + 5)) return;
  unsigned long long exact = (unsigned long long)a * (unsigned long long)b;
  int ov = (exact > ULONG_MAX);
  if (__builtin_mul_overflow(a,b,&res)) {
    assert(ov);
  } else {
    assert(!ov);
    assert(res == (unsigned long)exact);
  }
}

void test_mul_longlong() {
  long long a = __VERIFIER_nondet_longlong();
  long long b = __VERIFIER_nondet_longlong();
  long long res;

  // Range without overflow
  if (!(a >= LL_SQRT_FLOOR - 10 && a <= LL_SQRT_FLOOR)) return;
  if (!(b >= LL_SQRT_FLOOR - 10 && b <= LL_SQRT_FLOOR)) return;
  if (__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Range with guaranteed overflow
  a = __VERIFIER_nondet_longlong();
  b = __VERIFIER_nondet_longlong();
  if (!(a >= LL_SQRT_FLOOR + 1 && a <= LL_SQRT_FLOOR + 20)) return;
  if (!(b >= LL_SQRT_FLOOR + 1 && b <= LL_SQRT_FLOOR + 20)) return;
  if (!__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Mixed boundary range
  a = __VERIFIER_nondet_longlong();
  b = __VERIFIER_nondet_longlong();
  if (!(a >= LL_SQRT_FLOOR - 5 && a <= LL_SQRT_FLOOR + 5)) return;
  if (!(b >= LL_SQRT_FLOOR - 5 && b <= LL_SQRT_FLOOR + 5)) return;
  __int128 exact = (__int128)a * (__int128)b;
  int ov = (exact > LLONG_MAX || exact < LLONG_MIN);
  if (__builtin_mul_overflow(a,b,&res)) {
    assert(ov);
  } else {
    assert(!ov);
    assert(res == (long long)exact);
  }
}

void test_mul_ulonglong() {
  unsigned long long a = __VERIFIER_nondet_ulonglong();
  unsigned long long b = __VERIFIER_nondet_ulonglong();
  unsigned long long res;

  // Range without overflow
  if (!(a >= ULL_SQRT_FLOOR - 10 && a <= ULL_SQRT_FLOOR)) return;
  if (!(b >= ULL_SQRT_FLOOR - 10 && b <= ULL_SQRT_FLOOR)) return;
  if (__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Range with guaranteed overflow
  a = __VERIFIER_nondet_ulonglong();
  b = __VERIFIER_nondet_ulonglong();
  if (!(a >= ULL_SQRT_FLOOR + 1 && a <= ULL_SQRT_FLOOR + 20)) return;
  if (!(b >= ULL_SQRT_FLOOR + 1 && b <= ULL_SQRT_FLOOR + 20)) return;
  if (!__builtin_mul_overflow(a,b,&res)) { assert(0); }

  // Mixed boundary range
  a = __VERIFIER_nondet_ulonglong();
  b = __VERIFIER_nondet_ulonglong();
  if (!(a >= ULL_SQRT_FLOOR - 5 && a <= ULL_SQRT_FLOOR + 5)) return;
  if (!(b >= ULL_SQRT_FLOOR - 5 && b <= ULL_SQRT_FLOOR + 5)) return;
  __uint128_t exact = (__uint128_t)a * (__uint128_t)b;
  int ov = (exact > ULLONG_MAX);
  if (__builtin_mul_overflow(a,b,&res)) {
    assert(ov);
  } else {
    assert(!ov);
    assert(res == (unsigned long long)exact);
  }
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