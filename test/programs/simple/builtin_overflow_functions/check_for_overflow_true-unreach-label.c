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
    // __builtin_add_overflow
    {
        // calculations are done with infinite precision (parameters are not casted to another type):
        int a; long long c;
        shouldBeTrue(__builtin_add_overflow(INT_MAX + 1l, INT_MAX, &a))
        shouldBeFalse(__builtin_add_overflow(INT_MAX + 1l, -100, &a))
        shouldBeFalse(__builtin_add_overflow(INT_MAX, 1, &c))
    }

    // __builtin_sadd_overflow
    {
        int a;
        shouldBeFalse(__builtin_sadd_overflow(INT_MAX - 1, 1, &a))
        shouldBeFalse(__builtin_sadd_overflow(INT_MIN + 1, -1, &a))
        shouldBeTrue(__builtin_sadd_overflow(INT_MAX, 1, &a))
        shouldBeTrue(__builtin_sadd_overflow(INT_MIN, -1, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_sadd_overflow(INT_MAX + 1l, INT_MAX, &a))
        shouldBeFalse(__builtin_sadd_overflow(UINT_MAX, INT_MAX, &a))
        shouldBeTrue(__builtin_sadd_overflow(INT_MAX + 1l, -100, &a))

    }

    // __builtin_saddl_overflow
    {
        long a;
        shouldBeFalse(__builtin_saddl_overflow(LONG_MAX - 1l, 1l, &a))
        shouldBeFalse(__builtin_saddl_overflow(LONG_MIN + 1l, -1l, &a))
        shouldBeTrue(__builtin_saddl_overflow(LONG_MAX, 1l, &a))
        shouldBeTrue(__builtin_saddl_overflow(LONG_MIN, -1l, &a))

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
        shouldBeFalse(__builtin_uadd_overflow(UINT_MAX - 1u, 1u, &a))
        shouldBeTrue(__builtin_uadd_overflow(UINT_MAX, 1u, &a))

        // check proper type conversion of arguments
        shouldBeFalse(__builtin_uadd_overflow(UINT_MAX + 1ul, 1u, &a))
        shouldBeTrue(__builtin_uadd_overflow(-INT_MAX, INT_MAX, &a))
    }

    // __builtin_uaddl_overflow
    {
        unsigned long a;
        shouldBeFalse(__builtin_uaddl_overflow(ULONG_MAX - 1ul, 1ul, &a))
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
        shouldBeTrue(__builtin_sub_overflow(INT_MIN - 1l, INT_MAX, &a))
        shouldBeFalse(__builtin_sub_overflow(INT_MIN - 1l, -100, &a))
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
        shouldBeFalse(__builtin_ssub_overflow(INT_MAX + 1l, INT_MIN, &a))
        shouldBeFalse(__builtin_ssub_overflow(UINT_MAX, INT_MIN, &a))
        shouldBeTrue(__builtin_ssub_overflow(INT_MAX + 1l, 100, &a))
    }
    
    // __builtin_ssubl_overflow
    {
        long a;
        shouldBeFalse(__builtin_ssubl_overflow(LONG_MAX - 1l, -1l, &a))
        shouldBeFalse(__builtin_ssubl_overflow(LONG_MIN + 1l, 1l, &a))
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
        shouldBeTrue(__builtin_usub_overflow(UINT_MAX + 1ul, 1u, &a))
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

    return 0;

    ERROR:
        return 1;
}
