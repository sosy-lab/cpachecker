// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

# 1 "builtin_overflow_side_effect_result.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 31 "<command-line>"
# 1 "/usr/include/stdc-predef.h" 1 3 4
# 32 "<command-line>" 2
# 1 "builtin_overflow_side_effect_result.c"
# 9 "builtin_overflow_side_effect_result.c"
# 1 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 1 3 4
# 34 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 3 4
# 1 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/syslimits.h" 1 3 4






# 1 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 1 3 4
# 194 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 3 4
# 1 "/usr/include/limits.h" 1 3 4
# 26 "/usr/include/limits.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/libc-header-start.h" 1 3 4
# 33 "/usr/include/x86_64-linux-gnu/bits/libc-header-start.h" 3 4
# 1 "/usr/include/features.h" 1 3 4
# 424 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 1 3 4
# 427 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/wordsize.h" 1 3 4
# 428 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 2 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/long-double.h" 1 3 4
# 429 "/usr/include/x86_64-linux-gnu/sys/cdefs.h" 2 3 4
# 425 "/usr/include/features.h" 2 3 4
# 448 "/usr/include/features.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 1 3 4
# 10 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/gnu/stubs-64.h" 1 3 4
# 11 "/usr/include/x86_64-linux-gnu/gnu/stubs.h" 2 3 4
# 449 "/usr/include/features.h" 2 3 4
# 34 "/usr/include/x86_64-linux-gnu/bits/libc-header-start.h" 2 3 4
# 27 "/usr/include/limits.h" 2 3 4
# 183 "/usr/include/limits.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/posix1_lim.h" 1 3 4
# 160 "/usr/include/x86_64-linux-gnu/bits/posix1_lim.h" 3 4
# 1 "/usr/include/x86_64-linux-gnu/bits/local_lim.h" 1 3 4
# 38 "/usr/include/x86_64-linux-gnu/bits/local_lim.h" 3 4
# 1 "/usr/include/linux/limits.h" 1 3 4
# 39 "/usr/include/x86_64-linux-gnu/bits/local_lim.h" 2 3 4
# 161 "/usr/include/x86_64-linux-gnu/bits/posix1_lim.h" 2 3 4
# 184 "/usr/include/limits.h" 2 3 4



# 1 "/usr/include/x86_64-linux-gnu/bits/posix2_lim.h" 1 3 4
# 188 "/usr/include/limits.h" 2 3 4
# 195 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 2 3 4
# 8 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/syslimits.h" 2 3 4
# 35 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 2 3 4
# 10 "builtin_overflow_side_effect_result.c" 2
# 18 "builtin_overflow_side_effect_result.c"
int main()
{

    {
        int a; long long c;

        __builtin_add_overflow(0x7fffffff, 0x7fffffff, &c);
        if (!(c == 2LL * 0x7fffffff)) { goto ERROR; }

        int x = 0x7fffffff, y = 0x7fffffff;
        __builtin_add_overflow(x, y, &c);
        if (!(c == 1LL*x + y)) { goto ERROR; }


        __builtin_add_overflow(0x7fffffff, 1, &a);
        if (!(a == 
# 33 "builtin_overflow_side_effect_result.c" 3 4
       (-0x7fffffff - 1)
# 33 "builtin_overflow_side_effect_result.c"
       )) { goto ERROR; }
    }


    {
        int a;

        __builtin_sadd_overflow(0x7fffffff -1, 1, &a);
        if (!(a == 0x7fffffff)) { goto ERROR; }


        __builtin_sadd_overflow(0x7fffffff, 4, &a);
        if (!(a == 
# 45 "builtin_overflow_side_effect_result.c" 3 4
       (-0x7fffffff - 1)
# 45 "builtin_overflow_side_effect_result.c"
       +3)) { goto ERROR; }


        __builtin_sadd_overflow(0x7fffffff + 1l, -100, &a);
        if (!(a == 0x7fffffff - 99)) { goto ERROR; }
    }


    {
        long a;
        __builtin_saddl_overflow(0x7fffffffffffffffL - 1l, 1l, &a);
        if (!(a == 0x7fffffffffffffffL)) { goto ERROR; }

        int c;
        __builtin_saddl_overflow(0x7fffffffffffffffL - 1l, 1l, &c);
        if (!(c == -1)) { goto ERROR; }

        __builtin_saddl_overflow(0x7fffffffffffffffL, 4l, &a);
        if (!(a == 
# 63 "builtin_overflow_side_effect_result.c" 3 4
       (-0x7fffffffffffffffL - 1L) 
# 63 "builtin_overflow_side_effect_result.c"
       + 3l)) { goto ERROR; }

        __builtin_saddl_overflow(0x7fffffffffffffffL + 1LL, -100l, &a);
        if (!(a == 0x7fffffffffffffffL - 99l)) { goto ERROR; }
    }


    {
        long long a;
        __builtin_saddll_overflow(0x7fffffffffffffffLL - 1LL, 1LL, &a);
        if (!(a == 0x7fffffffffffffffLL)) { goto ERROR; }

        __builtin_saddll_overflow(0x7fffffffffffffffLL, 4LL, &a);
        if (!(a == 
# 76 "builtin_overflow_side_effect_result.c" 3 4
       (-0x7fffffffffffffffL - 1L) 
# 76 "builtin_overflow_side_effect_result.c"
       + 3LL)) { goto ERROR; }
    }


    {
        unsigned int a;
        __builtin_uadd_overflow(
# 82 "builtin_overflow_side_effect_result.c" 3 4
                               (0x7fffffff * 2U + 1U) 
# 82 "builtin_overflow_side_effect_result.c"
                                        - 1u, 1u, &a);
        if (!(a == 
# 83 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffff * 2U + 1U)
# 83 "builtin_overflow_side_effect_result.c"
       )) { goto ERROR; }

        __builtin_uadd_overflow(
# 85 "builtin_overflow_side_effect_result.c" 3 4
                               (0x7fffffff * 2U + 1U)
# 85 "builtin_overflow_side_effect_result.c"
                                       , 4u, &a);
        if (!(a == 3u)) { goto ERROR; }

        __builtin_uadd_overflow(
# 88 "builtin_overflow_side_effect_result.c" 3 4
                               (0x7fffffff * 2U + 1U) 
# 88 "builtin_overflow_side_effect_result.c"
                                        + 1ul, -100u, &a);
        if (!(a == 
# 89 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 89 "builtin_overflow_side_effect_result.c"
       - 99u)) { goto ERROR; }

    }


    {
        unsigned long a;
        __builtin_uaddl_overflow(
# 96 "builtin_overflow_side_effect_result.c" 3 4
                                (0x7fffffffffffffffL * 2UL + 1UL) 
# 96 "builtin_overflow_side_effect_result.c"
                                          - 1ul, 1ul, &a);
        if (!(a == 
# 97 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL)
# 97 "builtin_overflow_side_effect_result.c"
       )) { goto ERROR; }

        __builtin_uaddl_overflow(
# 99 "builtin_overflow_side_effect_result.c" 3 4
                                (0x7fffffffffffffffL * 2UL + 1UL)
# 99 "builtin_overflow_side_effect_result.c"
                                         , 4ul, &a);
        if (!(a == 3ul)) { goto ERROR; }

        __builtin_uaddl_overflow(
# 102 "builtin_overflow_side_effect_result.c" 3 4
                                (0x7fffffffffffffffL * 2UL + 1UL) 
# 102 "builtin_overflow_side_effect_result.c"
                                          + 1ull, -100ul, &a);
        if (!(a == 
# 103 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 103 "builtin_overflow_side_effect_result.c"
       - 99ul)) { goto ERROR; }
    }



    {
        unsigned long long a;
        __builtin_uaddll_overflow(
# 110 "builtin_overflow_side_effect_result.c" 3 4
                                 (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 110 "builtin_overflow_side_effect_result.c"
                                            - 1uLL, 1uLL, &a);
        if (!(a == 
# 111 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL)
# 111 "builtin_overflow_side_effect_result.c"
       )) { goto ERROR; }

        __builtin_uaddll_overflow(
# 113 "builtin_overflow_side_effect_result.c" 3 4
                                 (0x7fffffffffffffffLL * 2ULL + 1ULL)
# 113 "builtin_overflow_side_effect_result.c"
                                           , 4uLL, &a);
        if (!(a == 0uLL + 3uLL)) { goto ERROR; }
    }


    {
        int a; long long c;

        __builtin_sub_overflow(
# 121 "builtin_overflow_side_effect_result.c" 3 4
                              (-0x7fffffff - 1)
# 121 "builtin_overflow_side_effect_result.c"
                                     , 100, &c);
        if (!(c == 
# 122 "builtin_overflow_side_effect_result.c" 3 4
       (-0x7fffffff - 1) 
# 122 "builtin_overflow_side_effect_result.c"
       - 100ll)) { goto ERROR; }


        __builtin_sub_overflow(
# 125 "builtin_overflow_side_effect_result.c" 3 4
                              (-0x7fffffff - 1)
# 125 "builtin_overflow_side_effect_result.c"
                                     , 1, &a);
        if (!(a == 0x7fffffff)) { goto ERROR; }
    }


    {
        int a;
        __builtin_ssub_overflow(
# 132 "builtin_overflow_side_effect_result.c" 3 4
                               (-0x7fffffff - 1) 
# 132 "builtin_overflow_side_effect_result.c"
                                       + 1, 1, &a);
        if (!(a == 
# 133 "builtin_overflow_side_effect_result.c" 3 4
       (-0x7fffffff - 1)
# 133 "builtin_overflow_side_effect_result.c"
       )) { goto ERROR; }

        __builtin_ssub_overflow(
# 135 "builtin_overflow_side_effect_result.c" 3 4
                               (-0x7fffffff - 1)
# 135 "builtin_overflow_side_effect_result.c"
                                      , 4, &a);
        if (!(a == 0x7fffffff - 3)) { goto ERROR; }

        __builtin_ssub_overflow(0x7fffffff + 1l, 100, &a);
        if (!(a == 0x7fffffff - 99)) { goto ERROR; }
    }


    {
        long a;
        __builtin_ssubl_overflow(
# 145 "builtin_overflow_side_effect_result.c" 3 4
                                (-0x7fffffffffffffffL - 1L) 
# 145 "builtin_overflow_side_effect_result.c"
                                         + 1l, 1l, &a);
        if (!(a == 
# 146 "builtin_overflow_side_effect_result.c" 3 4
       (-0x7fffffffffffffffL - 1L)
# 146 "builtin_overflow_side_effect_result.c"
       )) { goto ERROR; }

        __builtin_ssubl_overflow(
# 148 "builtin_overflow_side_effect_result.c" 3 4
                                (-0x7fffffffffffffffL - 1L)
# 148 "builtin_overflow_side_effect_result.c"
                                        , 4l, &a);
        if (!(a == 0x7fffffffffffffffL - 3l)) { goto ERROR; }

        __builtin_ssubl_overflow(0x7fffffffffffffffL + 1ll, 100l, &a);
        if (!(a == 0x7fffffffffffffffL - 99l)) { goto ERROR; }
    }


    {
        long long a;
        __builtin_ssubll_overflow(
# 158 "builtin_overflow_side_effect_result.c" 3 4
                                 (-0x7fffffffffffffffLL - 1LL) 
# 158 "builtin_overflow_side_effect_result.c"
                                           + 1ll, 1ll, &a);
        if (!(a == 
# 159 "builtin_overflow_side_effect_result.c" 3 4
       (-0x7fffffffffffffffLL - 1LL)
# 159 "builtin_overflow_side_effect_result.c"
       )) { goto ERROR; }

        __builtin_ssubll_overflow(
# 161 "builtin_overflow_side_effect_result.c" 3 4
                                 (-0x7fffffffffffffffLL - 1LL)
# 161 "builtin_overflow_side_effect_result.c"
                                          , 4ll, &a);
        if (!(a == 0x7fffffffffffffffLL - 3ll)) { goto ERROR; }
    }


    {
        unsigned int a;
        __builtin_usub_overflow(1u, 1u, &a);
        if (!(a == 0u)) { goto ERROR; }

        __builtin_usub_overflow(0u, 4u, &a);
        if (!(a == 
# 172 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 172 "builtin_overflow_side_effect_result.c"
       - 3u)) { goto ERROR; }

        __builtin_usub_overflow(
# 174 "builtin_overflow_side_effect_result.c" 3 4
                               (0x7fffffff * 2U + 1U) 
# 174 "builtin_overflow_side_effect_result.c"
                                        + 1ul, 100u, &a);
        if (!(a == 
# 175 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 175 "builtin_overflow_side_effect_result.c"
       - 99u)) { goto ERROR; }
    }


    {
        unsigned long int a;
        __builtin_usubl_overflow(1ul, 1ul, &a);
        if (!(a == 0ul)) { goto ERROR; }

        __builtin_usubl_overflow(0ul, 4ul, &a);
        if (!(a == 
# 185 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 185 "builtin_overflow_side_effect_result.c"
       - 3ul)) { goto ERROR; }

        __builtin_usubl_overflow(
# 187 "builtin_overflow_side_effect_result.c" 3 4
                                (0x7fffffffffffffffL * 2UL + 1UL) 
# 187 "builtin_overflow_side_effect_result.c"
                                          + 1ull, 100ul, &a);
        if (!(a == 
# 188 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 188 "builtin_overflow_side_effect_result.c"
       - 99ul)) { goto ERROR; }
    }


    {
        unsigned long long int a;
        __builtin_usubll_overflow(1ull, 1ull, &a);
        if (!(a == 0ull)) { goto ERROR; }

        __builtin_usubll_overflow(0ull, 4ull, &a);
        if (!(a == 
# 198 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 198 "builtin_overflow_side_effect_result.c"
       - 3ull)) { goto ERROR; }
    }


    {
        int a; long long c;

        __builtin_mul_overflow(0x7fffffff, 2, &c);
        if (!(c == 0x7fffffff * 2ll)) { goto ERROR; }


        __builtin_mul_overflow(0x7fffffff, 2, &a);
        if (!(a == -2)) { goto ERROR; }
    }


    {
        int a;
        __builtin_smul_overflow(5, 2, &a);
        if (!(a == 10)) { goto ERROR; }

        __builtin_smul_overflow(0x7fffffff, 2, &a);
        if (!(a == -2)) { goto ERROR; }
    }


    {
        long a;
        __builtin_smull_overflow(5, 2, &a);
        if (!(a == 10)) { goto ERROR; }

        __builtin_smull_overflow(0x7fffffffffffffffL, 2, &a);
        if (!(a == -2)) { goto ERROR; }
    }


    {
        long long a;
        __builtin_smulll_overflow(5, 2, &a);
        if (!(a == 10)) { goto ERROR; }

        __builtin_smulll_overflow(0x7fffffffffffffffLL, 2, &a);
        if (!(a == -2)) { goto ERROR; }
    }


    {
        unsigned int a;
        __builtin_umul_overflow(5, 2, &a);
        if (!(a == 10)) { goto ERROR; }

        __builtin_umul_overflow(
# 249 "builtin_overflow_side_effect_result.c" 3 4
                               (0x7fffffff * 2U + 1U)
# 249 "builtin_overflow_side_effect_result.c"
                                       , 2, &a);
        if (!(a == 
# 250 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 250 "builtin_overflow_side_effect_result.c"
       - 1)) { goto ERROR; }
    }


    {
        unsigned long a;
        __builtin_umull_overflow(5, 2, &a);
        if (!(a == 10)) { goto ERROR; }

        __builtin_umull_overflow(
# 259 "builtin_overflow_side_effect_result.c" 3 4
                                (0x7fffffffffffffffL * 2UL + 1UL)
# 259 "builtin_overflow_side_effect_result.c"
                                         , 2, &a);
        if (!(a == 
# 260 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 260 "builtin_overflow_side_effect_result.c"
       - 1)) { goto ERROR; }
    }


    {
        unsigned long long a;
        __builtin_umulll_overflow(5, 2, &a);
        if (!(a == 10)) { goto ERROR; }

        __builtin_umulll_overflow(
# 269 "builtin_overflow_side_effect_result.c" 3 4
                                 (0x7fffffffffffffffLL * 2ULL + 1ULL)
# 269 "builtin_overflow_side_effect_result.c"
                                           , 2, &a);
        if (!(a == 
# 270 "builtin_overflow_side_effect_result.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 270 "builtin_overflow_side_effect_result.c"
       - 1)) { goto ERROR; }
    }

    return 0;

    ERROR:
      return 1;
}
