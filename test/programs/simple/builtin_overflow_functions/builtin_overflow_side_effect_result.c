// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <limits.h>

// macro simplifies test code
#define shouldBeTrue(a) \
    if (!(a))           \
    {                   \
        goto ERROR;     \
    }

// Test whether the side effect (=third parameter) is calculated correctly
int main()
{
    // __builtin_add_overflow
    {
        int a; long long c;
        // no overflow in type of c
        __builtin_add_overflow(INT_MAX, INT_MAX, &c);
        shouldBeTrue(c == 2LL * INT_MAX)

        int x = INT_MAX, y = INT_MAX;
        __builtin_add_overflow(x, y, &c);
        shouldBeTrue(c == 1LL*x + y)

        // overflow
        __builtin_add_overflow(INT_MAX, 1, &a);
        shouldBeTrue(a == INT_MIN)
    }

    // // __builtin_sadd_overflow
    {
        int a;
        // no overflow
        __builtin_sadd_overflow(INT_MAX-1ll, 1, &a);
        shouldBeTrue(a == INT_MAX)

        // overflow
        __builtin_sadd_overflow(INT_MAX, 4, &a);
        shouldBeTrue(a == INT_MIN+3)

        // overflow during parameter conversion and calculation
        __builtin_sadd_overflow(INT_MAX + 1ll, -100, &a);
        shouldBeTrue(a == INT_MAX - 99)
    }

    // __builtin_saddl_overflow
    {
        long a;
        __builtin_saddl_overflow(LONG_MAX - 1ll, 1l, &a);
        shouldBeTrue(a == LONG_MAX)

        __builtin_saddl_overflow(LONG_MAX, 4l, &a);
        shouldBeTrue(a == LONG_MIN + 3l)

        __builtin_saddl_overflow(LONG_MAX + 1LL, -100l, &a);
        shouldBeTrue(a == LONG_MAX - 99l)
    }

    // __builtin_saddll_overflow
    {
        long long a;
        __builtin_saddll_overflow(LLONG_MAX - 1LL, 1LL, &a);
        shouldBeTrue(a == LLONG_MAX)

        long c;
        __builtin_saddll_overflow(LLONG_MAX - 1ll, 1l, &c);
        shouldBeTrue(c == -1) // type conversion when writing to c

        __builtin_saddll_overflow(LLONG_MAX, 4LL, &a);
        shouldBeTrue(a == LLONG_MIN + 3LL)
    }

    // __builtin_uadd_overflow
    {
        unsigned int a;
        __builtin_uadd_overflow(UINT_MAX - 1ull, 1u, &a);
        shouldBeTrue(a == UINT_MAX)

        __builtin_uadd_overflow(UINT_MAX, 4u, &a);
        shouldBeTrue(a == 3u)

        __builtin_uadd_overflow(UINT_MAX + 1ull, -100u, &a);
        shouldBeTrue(a == UINT_MAX - 99u)

    }

    // __builtin_uaddl_overflow
    {
        unsigned long a;
        __builtin_uaddl_overflow(ULONG_MAX - 1ull, 1ul, &a);
        shouldBeTrue(a == ULONG_MAX)

        __builtin_uaddl_overflow(ULONG_MAX, 4ul, &a);
        shouldBeTrue(a == 3ul)

        __builtin_uaddl_overflow(ULONG_MAX + 1ull, -100ul, &a);
        shouldBeTrue(a == ULONG_MAX - 99ul)
    }


    // __builtin_uaddll_overflow
    {
        unsigned long long a;
        __builtin_uaddll_overflow(ULLONG_MAX - 1uLL, 1uLL, &a);
        shouldBeTrue(a == ULLONG_MAX)

        __builtin_uaddll_overflow(ULLONG_MAX, 4uLL, &a);
        shouldBeTrue(a == 0uLL + 3uLL)
    }

    // __builtin_sub_overflow
    {
        int a; long long c;
        // no overflow in type of c
        __builtin_sub_overflow(INT_MIN, 100, &c);
        shouldBeTrue(c == INT_MIN - 100ll)

        // overflow
        __builtin_sub_overflow(INT_MIN, 1, &a);
        shouldBeTrue(a == INT_MAX)
    }

    // __builtin_ssub_overflow
    {
        int a;
        __builtin_ssub_overflow(INT_MIN + 1ll, 1, &a);
        shouldBeTrue(a == INT_MIN)

        __builtin_ssub_overflow(INT_MIN, 4, &a);
        shouldBeTrue(a == INT_MAX - 3)

        __builtin_ssub_overflow(INT_MAX + 1ll, 100, &a);
        shouldBeTrue(a == INT_MAX - 99)
    }
    
    // __builtin_ssubl_overflow
    {
        long a;
        __builtin_ssubl_overflow(LONG_MIN + 1ll, 1l, &a);
        shouldBeTrue(a == LONG_MIN)

        __builtin_ssubl_overflow(LONG_MIN, 4l, &a);
        shouldBeTrue(a == LONG_MAX - 3l)

        __builtin_ssubl_overflow(LONG_MAX + 1ll, 100l, &a);
        shouldBeTrue(a == LONG_MAX - 99l)
    }

    // __builtin_ssubll_overflow
    {
        long long a;
        __builtin_ssubll_overflow(LLONG_MIN + 1ll, 1ll, &a);
        shouldBeTrue(a == LLONG_MIN)

        __builtin_ssubll_overflow(LLONG_MIN, 4ll, &a);
        shouldBeTrue(a == LLONG_MAX - 3ll)
    }

    // __builtin_usub_overflow
    {
        unsigned int a;
        __builtin_usub_overflow(1u, 1u, &a);
        shouldBeTrue(a == 0u)

        __builtin_usub_overflow(0u, 4u, &a);
        shouldBeTrue(a == UINT_MAX - 3u)

        __builtin_usub_overflow(UINT_MAX + 1ull, 100u, &a);
        shouldBeTrue(a == UINT_MAX - 99u)
    }

    // __builtin_usubl_overflow
    {
        unsigned long int a;
        __builtin_usubl_overflow(1ul, 1ul, &a);
        shouldBeTrue(a == 0ul)

        __builtin_usubl_overflow(0ul, 4ul, &a);
        shouldBeTrue(a == ULONG_MAX - 3ul)

        __builtin_usubl_overflow(ULONG_MAX + 1ull, 100ul, &a);
        shouldBeTrue(a == ULONG_MAX - 99ul)
    }

    // __builtin_usubll_overflow
    {
        unsigned long long int a;
        __builtin_usubll_overflow(1ull, 1ull, &a);
        shouldBeTrue(a == 0ull)

        __builtin_usubll_overflow(0ull, 4ull, &a);
        shouldBeTrue(a == ULLONG_MAX - 3ull)
    }

    // __builtin_mul_overflow
    {
        int a; long long c;
        // no overflow in type of c
        __builtin_mul_overflow(INT_MAX, 2, &c);
        shouldBeTrue(c == INT_MAX * 2ll)

        // overflow
        __builtin_mul_overflow(INT_MAX, 2, &a);
        shouldBeTrue(a == -2)
    }

    // __builtin_smul_overflow
    {
        int a;
        __builtin_smul_overflow(5, 2, &a);
        shouldBeTrue(a == 10)

        __builtin_smul_overflow(INT_MAX, 2, &a);
        shouldBeTrue(a == -2)
    }
    
    // __builtin_smull_overflow
    {
        long a;
        __builtin_smull_overflow(5, 2, &a);
        shouldBeTrue(a == 10)

        __builtin_smull_overflow(LONG_MAX, 2, &a);
        shouldBeTrue(a == -2)
    }

    // __builtin_smulll_overflow
    {
        long long a;
        __builtin_smulll_overflow(5, 2, &a);
        shouldBeTrue(a == 10)

        __builtin_smulll_overflow(LLONG_MAX, 2, &a);
        shouldBeTrue(a == -2)
    }

    // __builtin_umul_overflow
    {
        unsigned int a;
        __builtin_umul_overflow(5, 2, &a);
        shouldBeTrue(a == 10)

        __builtin_umul_overflow(UINT_MAX, 2, &a);
        shouldBeTrue(a == UINT_MAX - 1)
    }
    
    // __builtin_umull_overflow
    {
        unsigned long a;
        __builtin_umull_overflow(5, 2, &a);
        shouldBeTrue(a == 10)

        __builtin_umull_overflow(ULONG_MAX, 2, &a);
        shouldBeTrue(a == ULONG_MAX - 1)
    }

    // __builtin_umulll_overflow
    {
        unsigned long long a;
        __builtin_umulll_overflow(5, 2, &a);
        shouldBeTrue(a == 10)

        __builtin_umulll_overflow(ULLONG_MAX, 2, &a);
        shouldBeTrue(a == ULLONG_MAX - 1)
    }

    return 0;

    ERROR:
      return 1;
}
