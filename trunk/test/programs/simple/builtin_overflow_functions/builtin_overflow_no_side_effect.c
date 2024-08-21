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

int main() 
{ 
    // __builtin_add_overflow_p
    {
        // calculations are done with infinite precision (parameters are not casted to another type):
        int a; long long c;
        shouldBeTrue(__builtin_add_overflow_p(INT_MAX + 1ll, INT_MAX, a))
        shouldBeFalse(__builtin_add_overflow_p(INT_MAX + 1ll, -100, a))
        shouldBeFalse(__builtin_add_overflow_p(INT_MAX, 1, c))
        shouldBeTrue(__builtin_add_overflow_p(INT_MAX, 1, (int)c))
    }

    // __builtin_sub_overflow_p
    {
        // calculations are done with infinite precision (parameters are not casted to another type):
        int a; long long c;
        shouldBeTrue(__builtin_sub_overflow_p(INT_MIN - 1ll, INT_MAX, a))
        shouldBeFalse(__builtin_sub_overflow_p(INT_MIN - 1ll, -100, a))
        shouldBeFalse(__builtin_sub_overflow_p(INT_MIN, 1, c))
    }

    // __builtin_mul_overflow_p
    {
        int a; long long c;
        shouldBeTrue(__builtin_mul_overflow_p(INT_MAX + 1ll, 1, a))
        shouldBeTrue(__builtin_mul_overflow_p(INT_MIN - 1ll, -1, a))
        shouldBeFalse(__builtin_mul_overflow_p(INT_MAX, 2, c))
    }

    return 0;

    ERROR:
    return 1;
}
