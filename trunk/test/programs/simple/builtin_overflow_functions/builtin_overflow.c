// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <limits.h>

// macro simplifies test code
// (if a is not false -> Error)
#define shouldBeFalse(a) \
    if ((a))             \
    {                    \
        goto ERROR;      \
    }
#define shouldBeTrue(a) \
    if (!(a))           \
    {                   \
        goto ERROR;     \
    }

// Tests whether overflow (=return of function) is caluclated correctly
int main()
{
    // __builtin_add_overflow
    {
        // calculations are done with infinite precision (parameters are not casted to another type):
        int a; long long c;
        shouldBeTrue(__builtin_add_overflow(INT_MAX + 1ll, INT_MAX, &a))
        shouldBeFalse(__builtin_add_overflow(INT_MAX + 1ll, -100, &a))
        shouldBeFalse(__builtin_add_overflow(INT_MAX, 1, &c))

        int x = INT_MAX, y = 1;
        shouldBeFalse(__builtin_add_overflow(x, y, &c))
        x = INT_MAX, y = INT_MAX;
        shouldBeTrue(__builtin_add_overflow(x, y, &a))
    }

    // __builtin_sadd_overflow
    {
        int a;
        shouldBeFalse(__builtin_sadd_overflow(INT_MAX - 1ll, 1, &a))
        shouldBeFalse(__builtin_sadd_overflow(INT_MIN + 1ll, -1, &a))
        shouldBeTrue(__builtin_sadd_overflow(INT_MAX, 1, &a))
        shouldBeTrue(__builtin_sadd_overflow(INT_MIN, -1, &a))
        
        long long int c;
        shouldBeTrue(__builtin_sadd_overflow(INT_MAX, 1, &c))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_sadd_overflow(INT_MAX + 1ll, INT_MAX, &a))
        shouldBeFalse(__builtin_sadd_overflow(UINT_MAX, INT_MAX, &a))
        shouldBeTrue(__builtin_sadd_overflow(INT_MAX + 1ll, -100, &a))

    }

    // __builtin_saddl_overflow
    {
        long a;
        shouldBeFalse(__builtin_saddl_overflow(LONG_MAX - 1ll, 1l, &a))
        shouldBeFalse(__builtin_saddl_overflow(LONG_MIN + 1ll, -1l, &a))
        shouldBeTrue(__builtin_saddl_overflow(LONG_MAX, 1l, &a))
        shouldBeTrue(__builtin_saddl_overflow(LONG_MIN, -1l, &a))
        
        short c;
        shouldBeFalse(__builtin_saddl_overflow(SHRT_MAX, 1l, &c))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_saddl_overflow(LONG_MAX + 1LL, LONG_MAX, &a))
        shouldBeFalse(__builtin_saddl_overflow(ULONG_MAX, LONG_MAX, &a))
        shouldBeTrue(__builtin_saddl_overflow(LONG_MAX + 1LL, -100l, &a))
    }

    // __builtin_saddll_overflow
    {
        long long a;
        shouldBeFalse(__builtin_saddll_overflow(LLONG_MAX - 1LL, 1LL, &a))
        shouldBeFalse(__builtin_saddll_overflow(LLONG_MIN + 1LL, -1LL, &a))
        shouldBeTrue(__builtin_saddll_overflow(LLONG_MAX, 1LL, &a))
        shouldBeTrue(__builtin_saddll_overflow(LLONG_MIN, -1LL, &a))
    }

    // __builtin_uadd_overflow
    {
        unsigned int a;
        shouldBeFalse(__builtin_uadd_overflow(UINT_MAX - 1ull, 1u, &a))
        shouldBeTrue(__builtin_uadd_overflow(UINT_MAX, 1u, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_uadd_overflow(UINT_MAX + 1ull, 1u, &a))
        shouldBeTrue(__builtin_uadd_overflow(-INT_MAX, INT_MAX, &a))
    }

    // __builtin_uaddl_overflow
    {
        unsigned long a;
        shouldBeFalse(__builtin_uaddl_overflow(ULONG_MAX - 1ull, 1ul, &a))
        shouldBeTrue(__builtin_uaddl_overflow(ULONG_MAX, 1ul, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_uadd_overflow(ULONG_MAX + 1uLL, 1ul, &a))
        shouldBeTrue(__builtin_uadd_overflow(-LONG_MAX, LONG_MAX, &a))
    }


    // __builtin_uaddll_overflow
    {
        unsigned long long a;
        shouldBeFalse(__builtin_uaddll_overflow(ULLONG_MAX - 1uLL, 1uLL, &a))
        shouldBeTrue(__builtin_uaddll_overflow(ULLONG_MAX, 1uLL, &a))
    }

    // __builtin_sub_overflow
    {
        // calculations are done with infinite precision (parameters are not casted to another type):
        int a; long long c;
        shouldBeTrue(__builtin_sub_overflow(INT_MIN - 1ll, INT_MAX, &a))
        shouldBeFalse(__builtin_sub_overflow(INT_MIN - 1ll, -100, &a))
        shouldBeFalse(__builtin_sub_overflow(INT_MIN, 1, &c))
    }

    // __builtin_ssub_overflow
    {
        int a;
        shouldBeFalse(__builtin_ssub_overflow(INT_MAX - 1, -1, &a))
        shouldBeFalse(__builtin_ssub_overflow(INT_MIN + 1, 1, &a))
        shouldBeTrue(__builtin_ssub_overflow(INT_MAX, -1, &a))
        shouldBeTrue(__builtin_ssub_overflow(INT_MIN, 1, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_ssub_overflow(INT_MAX + 1ll, INT_MIN, &a))
        shouldBeFalse(__builtin_ssub_overflow(UINT_MAX, INT_MIN, &a))
        shouldBeTrue(__builtin_ssub_overflow(INT_MAX + 1ll, 100, &a))
    }
    
    // __builtin_ssubl_overflow
    {
        long a;
        shouldBeFalse(__builtin_ssubl_overflow(LONG_MAX - 1ll, -1l, &a))
        shouldBeFalse(__builtin_ssubl_overflow(LONG_MIN + 1ll, 1l, &a))
        shouldBeTrue(__builtin_ssubl_overflow(LONG_MAX, -1l, &a))
        shouldBeTrue(__builtin_ssubl_overflow(LONG_MIN, 1l, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_ssubl_overflow(LONG_MAX + 1LL, LONG_MIN, &a))
        shouldBeFalse(__builtin_ssubl_overflow(ULONG_MAX, LONG_MIN, &a))
        shouldBeTrue(__builtin_ssubl_overflow(LONG_MAX + 1LL, 100l, &a))
    }

    // __builtin_ssubll_overflow
    {
        long long a;
        shouldBeFalse(__builtin_ssubll_overflow(LLONG_MAX - 1LL, -1LL, &a))
        shouldBeFalse(__builtin_ssubll_overflow(LLONG_MIN + 1LL, 1LL, &a))
        shouldBeTrue(__builtin_ssubll_overflow(LLONG_MAX, -1LL, &a))
        shouldBeTrue(__builtin_ssubll_overflow(LLONG_MIN, 1LL, &a))
    }

    // __builtin_usub_overflow
    {
        unsigned int a;
        shouldBeFalse(__builtin_usub_overflow(1u, 1u, &a))
        shouldBeTrue(__builtin_usub_overflow(0u, 1u, &a))

        // check proper type conversion of arguments
        shouldBeTrue(__builtin_usub_overflow(UINT_MAX + 1ull, 1u, &a))
    }

    // __builtin_usubl_overflow
    {
        unsigned long int a;
        shouldBeFalse(__builtin_usubl_overflow(1ul, 1ul, &a))
        shouldBeTrue(__builtin_usubl_overflow(0ul, 1ul, &a))

        // check proper type conversion of arguments
        shouldBeTrue(__builtin_usub_overflow(ULONG_MAX + 1uLL, 1ul, &a))
    }

    // __builtin_usubll_overflow
    {
        unsigned long long int a;
        shouldBeFalse(__builtin_usubll_overflow(1uLL, 1uLL, &a))
        shouldBeTrue(__builtin_usubll_overflow(0uLL, 1uLL, &a))
    }

    // __builtin_mul_overflow
    {
        int a; long long c;
        shouldBeTrue(__builtin_mul_overflow(INT_MAX + 1ll, 1, &a))
        shouldBeTrue(__builtin_mul_overflow(INT_MIN - 1ll, -1, &a))
        shouldBeFalse(__builtin_mul_overflow(INT_MAX, 2, &c))
    }

    // __builtin_smul_overflow
    {
        int a;
        shouldBeFalse(__builtin_smul_overflow(INT_MAX >> 1, 2, &a))
        shouldBeFalse(__builtin_smul_overflow(INT_MIN >> 1, 2, &a))
        shouldBeTrue(__builtin_smul_overflow(INT_MAX, 2, &a))
        shouldBeTrue(__builtin_smul_overflow(INT_MIN, 2, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_smul_overflow(INT_MAX + 1ll, 1, &a))
        shouldBeFalse(__builtin_smul_overflow(1, INT_MAX + 1ll, &a))
    }

    // __builtin_smull_overflow
    {
        long a;
        shouldBeFalse(__builtin_smull_overflow(LONG_MAX >> 1, 2, &a))
        shouldBeFalse(__builtin_smull_overflow(LONG_MIN >> 1, 2, &a))
        shouldBeTrue(__builtin_smull_overflow(LONG_MAX, 2, &a))
        shouldBeTrue(__builtin_smull_overflow(LONG_MIN, 2, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_smull_overflow(LONG_MAX + 1ll, 1, &a))
        shouldBeFalse(__builtin_smull_overflow(1, LONG_MAX + 1ll, &a))
    }

    // __builtin_smulll_overflow
    {
        long long a;
        shouldBeFalse(__builtin_smulll_overflow(LLONG_MAX >> 1, 2, &a))
        shouldBeFalse(__builtin_smulll_overflow(LLONG_MIN >> 1, 2, &a))
        shouldBeTrue(__builtin_smulll_overflow(LLONG_MAX, 2, &a))
        shouldBeTrue(__builtin_smulll_overflow(LLONG_MIN, 2, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_smulll_overflow(LLONG_MAX + 1uLL, 1, &a))
        shouldBeFalse(__builtin_smulll_overflow(1, LLONG_MAX + 1uLL, &a))
    }

    // __builtin_umul_overflow
    {
        unsigned int a;
        shouldBeFalse(__builtin_umul_overflow(UINT_MAX >> 1, 2, &a))
        shouldBeTrue(__builtin_umul_overflow(UINT_MAX, 2, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_umul_overflow(UINT_MAX + 1ull, 1, &a))
        shouldBeFalse(__builtin_umul_overflow(1, UINT_MAX + 1ull, &a))
    }

    // __builtin_umull_overflow
    {
        unsigned long a;
        shouldBeFalse(__builtin_umull_overflow(ULONG_MAX >> 1, 2, &a))
        shouldBeTrue(__builtin_umull_overflow(ULONG_MAX, 2, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_umull_overflow(ULONG_MAX + 1ull, 1, &a))
        shouldBeFalse(__builtin_umull_overflow(1, ULONG_MAX + 1ull, &a))
    }

    // __builtin_smulll_overflow
    {
        unsigned long long a;
        shouldBeFalse(__builtin_umulll_overflow(ULLONG_MAX >> 1, 2, &a))
        shouldBeTrue(__builtin_umulll_overflow(ULLONG_MAX, 2, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_umulll_overflow(ULLONG_MAX + 1uLL, 1, &a))
        shouldBeFalse(__builtin_umulll_overflow(1, ULLONG_MAX + 1uLL, &a))
    }

    return 0;

    ERROR:
    return 1;
}
