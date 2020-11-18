// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

# 1 "builtin_overflow.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 31 "<command-line>"
# 1 "/usr/include/stdc-predef.h" 1 3 4
# 32 "<command-line>" 2
# 1 "builtin_overflow.c"
# 9 "builtin_overflow.c"
# 1 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 1 3 4
# 34 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 3 4
# 1 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/syslimits.h" 1 3 4






# 1 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 1 3 4
# 194 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 3 4
# 1 "/usr/include/limits.h" 1 3 4
# 26 "/usr/include/limits.h" 3 4
# 1 "/usr/include/bits/libc-header-start.h" 1 3 4
# 33 "/usr/include/bits/libc-header-start.h" 3 4
# 1 "/usr/include/features.h" 1 3 4
# 424 "/usr/include/features.h" 3 4
# 1 "/usr/include/sys/cdefs.h" 1 3 4
# 427 "/usr/include/sys/cdefs.h" 3 4
# 1 "/usr/include/bits/wordsize.h" 1 3 4
# 428 "/usr/include/sys/cdefs.h" 2 3 4
# 1 "/usr/include/bits/long-double.h" 1 3 4
# 429 "/usr/include/sys/cdefs.h" 2 3 4
# 425 "/usr/include/features.h" 2 3 4
# 448 "/usr/include/features.h" 3 4
# 1 "/usr/include/gnu/stubs.h" 1 3 4






# 1 "/usr/include/gnu/stubs-32.h" 1 3 4
# 8 "/usr/include/gnu/stubs.h" 2 3 4
# 449 "/usr/include/features.h" 2 3 4
# 34 "/usr/include/bits/libc-header-start.h" 2 3 4
# 27 "/usr/include/limits.h" 2 3 4
# 183 "/usr/include/limits.h" 3 4
# 1 "/usr/include/bits/posix1_lim.h" 1 3 4
# 160 "/usr/include/bits/posix1_lim.h" 3 4
# 1 "/usr/include/bits/local_lim.h" 1 3 4
# 38 "/usr/include/bits/local_lim.h" 3 4
# 1 "/usr/include/linux/limits.h" 1 3 4
# 39 "/usr/include/bits/local_lim.h" 2 3 4
# 161 "/usr/include/bits/posix1_lim.h" 2 3 4
# 184 "/usr/include/limits.h" 2 3 4



# 1 "/usr/include/bits/posix2_lim.h" 1 3 4
# 188 "/usr/include/limits.h" 2 3 4
# 195 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 2 3 4
# 8 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/syslimits.h" 2 3 4
# 35 "/usr/lib/gcc/x86_64-linux-gnu/7/include-fixed/limits.h" 2 3 4
# 10 "builtin_overflow.c" 2
# 25 "builtin_overflow.c"
int main()
{

    {

        int a; long long c;
        if (!(__builtin_add_overflow(0x7fffffff + 1ll, 0x7fffffff, &a))) { goto ERROR; }
        if ((__builtin_add_overflow(0x7fffffff + 1ll, -100, &a))) { goto ERROR; }
        if ((__builtin_add_overflow(0x7fffffff, 1, &c))) { goto ERROR; }

        int x = 0x7fffffff, y = 1;
        if ((__builtin_add_overflow(x, y, &c))) { goto ERROR; }
        x = 0x7fffffff, y = 0x7fffffff;
        if (!(__builtin_add_overflow(x, y, &a))) { goto ERROR; }
    }


    {
        int a;
        if ((__builtin_sadd_overflow(0x7fffffff - 1ll, 1, &a))) { goto ERROR; }
        if ((__builtin_sadd_overflow(
# 45 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1) 
# 45 "builtin_overflow.c"
       + 1ll, -1, &a))) { goto ERROR; }
        if (!(__builtin_sadd_overflow(0x7fffffff, 1, &a))) { goto ERROR; }
        if (!(__builtin_sadd_overflow(
# 47 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1)
# 47 "builtin_overflow.c"
       , -1, &a))) { goto ERROR; }

        long long int c;
        if (!(__builtin_sadd_overflow(0x7fffffff, 1, &c))) { goto ERROR; }


        if ((__builtin_sadd_overflow(0x7fffffff + 1ll, 0x7fffffff, &a))) { goto ERROR; }
        if ((__builtin_sadd_overflow(
# 54 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U)
# 54 "builtin_overflow.c"
       , 0x7fffffff, &a))) { goto ERROR; }
        if (!(__builtin_sadd_overflow(0x7fffffff + 1ll, -100, &a))) { goto ERROR; }

    }


    {
        long a;
        if ((__builtin_saddl_overflow(0x7fffffffL - 1ll, 1l, &a))) { goto ERROR; }
        if ((__builtin_saddl_overflow(
# 63 "builtin_overflow.c" 3 4
       (-0x7fffffffL - 1L) 
# 63 "builtin_overflow.c"
       + 1ll, -1l, &a))) { goto ERROR; }
        if (!(__builtin_saddl_overflow(0x7fffffffL, 1l, &a))) { goto ERROR; }
        if (!(__builtin_saddl_overflow(
# 65 "builtin_overflow.c" 3 4
       (-0x7fffffffL - 1L)
# 65 "builtin_overflow.c"
       , -1l, &a))) { goto ERROR; }

        short c;
        if ((__builtin_saddl_overflow(0x7fff, 1l, &c))) { goto ERROR; }


        if ((__builtin_saddl_overflow(0x7fffffffL + 1LL, 0x7fffffffL, &a))) { goto ERROR; }
        if ((__builtin_saddl_overflow(
# 72 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL)
# 72 "builtin_overflow.c"
       , 0x7fffffffL, &a))) { goto ERROR; }
        if (!(__builtin_saddl_overflow(0x7fffffffL + 1LL, -100l, &a))) { goto ERROR; }
    }


    {
        long long a;
        if ((__builtin_saddll_overflow(0x7fffffffffffffffLL - 1LL, 1LL, &a))) { goto ERROR; }
        if ((__builtin_saddll_overflow(
# 80 "builtin_overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL) 
# 80 "builtin_overflow.c"
       + 1LL, -1LL, &a))) { goto ERROR; }
        if (!(__builtin_saddll_overflow(0x7fffffffffffffffLL, 1LL, &a))) { goto ERROR; }
        if (!(__builtin_saddll_overflow(
# 82 "builtin_overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL)
# 82 "builtin_overflow.c"
       , -1LL, &a))) { goto ERROR; }
    }


    {
        unsigned int a;
        if ((__builtin_uadd_overflow(
# 88 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 88 "builtin_overflow.c"
       - 1ull, 1u, &a))) { goto ERROR; }
        if (!(__builtin_uadd_overflow(
# 89 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U)
# 89 "builtin_overflow.c"
       , 1u, &a))) { goto ERROR; }


        if ((__builtin_uadd_overflow(
# 92 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 92 "builtin_overflow.c"
       + 1ull, 1u, &a))) { goto ERROR; }
        if (!(__builtin_uadd_overflow(-0x7fffffff, 0x7fffffff, &a))) { goto ERROR; }
    }


    {
        unsigned long a;
        if ((__builtin_uaddl_overflow(
# 99 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL) 
# 99 "builtin_overflow.c"
       - 1ull, 1ul, &a))) { goto ERROR; }
        if (!(__builtin_uaddl_overflow(
# 100 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL)
# 100 "builtin_overflow.c"
       , 1ul, &a))) { goto ERROR; }


        if ((__builtin_uadd_overflow(
# 103 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL) 
# 103 "builtin_overflow.c"
       + 1uLL, 1ul, &a))) { goto ERROR; }
        if (!(__builtin_uadd_overflow(-0x7fffffffL, 0x7fffffffL, &a))) { goto ERROR; }
    }



    {
        unsigned long long a;
        if ((__builtin_uaddll_overflow(
# 111 "builtin_overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 111 "builtin_overflow.c"
       - 1uLL, 1uLL, &a))) { goto ERROR; }
        if (!(__builtin_uaddll_overflow(
# 112 "builtin_overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL)
# 112 "builtin_overflow.c"
       , 1uLL, &a))) { goto ERROR; }
    }


    {

        int a; long long c;
        if (!(__builtin_sub_overflow(
# 119 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1) 
# 119 "builtin_overflow.c"
       - 1ll, 0x7fffffff, &a))) { goto ERROR; }
        if ((__builtin_sub_overflow(
# 120 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1) 
# 120 "builtin_overflow.c"
       - 1ll, -100, &a))) { goto ERROR; }
        if ((__builtin_sub_overflow(
# 121 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1)
# 121 "builtin_overflow.c"
       , 1, &c))) { goto ERROR; }
    }


    {
        int a;
        if ((__builtin_ssub_overflow(0x7fffffff - 1, -1, &a))) { goto ERROR; }
        if ((__builtin_ssub_overflow(
# 128 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1) 
# 128 "builtin_overflow.c"
       + 1, 1, &a))) { goto ERROR; }
        if (!(__builtin_ssub_overflow(0x7fffffff, -1, &a))) { goto ERROR; }
        if (!(__builtin_ssub_overflow(
# 130 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1)
# 130 "builtin_overflow.c"
       , 1, &a))) { goto ERROR; }


        if ((__builtin_ssub_overflow(0x7fffffff + 1ll, 
# 133 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1)
# 133 "builtin_overflow.c"
       , &a))) { goto ERROR; }
        if ((__builtin_ssub_overflow(
# 134 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U)
# 134 "builtin_overflow.c"
       , 
# 134 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1)
# 134 "builtin_overflow.c"
       , &a))) { goto ERROR; }
        if (!(__builtin_ssub_overflow(0x7fffffff + 1ll, 100, &a))) { goto ERROR; }
    }


    {
        long a;
        if ((__builtin_ssubl_overflow(0x7fffffffL - 1ll, -1l, &a))) { goto ERROR; }
        if ((__builtin_ssubl_overflow(
# 142 "builtin_overflow.c" 3 4
       (-0x7fffffffL - 1L) 
# 142 "builtin_overflow.c"
       + 1ll, 1l, &a))) { goto ERROR; }
        if (!(__builtin_ssubl_overflow(0x7fffffffL, -1l, &a))) { goto ERROR; }
        if (!(__builtin_ssubl_overflow(
# 144 "builtin_overflow.c" 3 4
       (-0x7fffffffL - 1L)
# 144 "builtin_overflow.c"
       , 1l, &a))) { goto ERROR; }


        if ((__builtin_ssubl_overflow(0x7fffffffL + 1LL, 
# 147 "builtin_overflow.c" 3 4
       (-0x7fffffffL - 1L)
# 147 "builtin_overflow.c"
       , &a))) { goto ERROR; }
        if ((__builtin_ssubl_overflow(
# 148 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL)
# 148 "builtin_overflow.c"
       , 
# 148 "builtin_overflow.c" 3 4
       (-0x7fffffffL - 1L)
# 148 "builtin_overflow.c"
       , &a))) { goto ERROR; }
        if (!(__builtin_ssubl_overflow(0x7fffffffL + 1LL, 100l, &a))) { goto ERROR; }
    }


    {
        long long a;
        if ((__builtin_ssubll_overflow(0x7fffffffffffffffLL - 1LL, -1LL, &a))) { goto ERROR; }
        if ((__builtin_ssubll_overflow(
# 156 "builtin_overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL) 
# 156 "builtin_overflow.c"
       + 1LL, 1LL, &a))) { goto ERROR; }
        if (!(__builtin_ssubll_overflow(0x7fffffffffffffffLL, -1LL, &a))) { goto ERROR; }
        if (!(__builtin_ssubll_overflow(
# 158 "builtin_overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL)
# 158 "builtin_overflow.c"
       , 1LL, &a))) { goto ERROR; }
    }


    {
        unsigned int a;
        if ((__builtin_usub_overflow(1u, 1u, &a))) { goto ERROR; }
        if (!(__builtin_usub_overflow(0u, 1u, &a))) { goto ERROR; }


        if (!(__builtin_usub_overflow(
# 168 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 168 "builtin_overflow.c"
       + 1ull, 1u, &a))) { goto ERROR; }
    }


    {
        unsigned long int a;
        if ((__builtin_usubl_overflow(1ul, 1ul, &a))) { goto ERROR; }
        if (!(__builtin_usubl_overflow(0ul, 1ul, &a))) { goto ERROR; }


        if (!(__builtin_usub_overflow(
# 178 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL) 
# 178 "builtin_overflow.c"
       + 1uLL, 1ul, &a))) { goto ERROR; }
    }


    {
        unsigned long long int a;
        if ((__builtin_usubll_overflow(1uLL, 1uLL, &a))) { goto ERROR; }
        if (!(__builtin_usubll_overflow(0uLL, 1uLL, &a))) { goto ERROR; }
    }


    {
        int a; long long c;
        if (!(__builtin_mul_overflow(0x7fffffff + 1ll, 1, &a))) { goto ERROR; }
        if (!(__builtin_mul_overflow(
# 192 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1) 
# 192 "builtin_overflow.c"
       - 1ll, -1, &a))) { goto ERROR; }
        if ((__builtin_mul_overflow(0x7fffffff, 2, &c))) { goto ERROR; }
    }


    {
        int a;
        if ((__builtin_smul_overflow(0x7fffffff >> 1, 2, &a))) { goto ERROR; }
        if ((__builtin_smul_overflow(
# 200 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1) 
# 200 "builtin_overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_smul_overflow(0x7fffffff, 2, &a))) { goto ERROR; }
        if (!(__builtin_smul_overflow(
# 202 "builtin_overflow.c" 3 4
       (-0x7fffffff - 1)
# 202 "builtin_overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_smul_overflow(0x7fffffff + 1ll, 1, &a))) { goto ERROR; }
        if ((__builtin_smul_overflow(1, 0x7fffffff + 1ll, &a))) { goto ERROR; }
    }


    {
        long a;
        if ((__builtin_smull_overflow(0x7fffffffL >> 1, 2, &a))) { goto ERROR; }
        if ((__builtin_smull_overflow(
# 213 "builtin_overflow.c" 3 4
       (-0x7fffffffL - 1L) 
# 213 "builtin_overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_smull_overflow(0x7fffffffL, 2, &a))) { goto ERROR; }
        if (!(__builtin_smull_overflow(
# 215 "builtin_overflow.c" 3 4
       (-0x7fffffffL - 1L)
# 215 "builtin_overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_smull_overflow(0x7fffffffL + 1ll, 1, &a))) { goto ERROR; }
        if ((__builtin_smull_overflow(1, 0x7fffffffL + 1ll, &a))) { goto ERROR; }
    }


    {
        long long a;
        if ((__builtin_smulll_overflow(0x7fffffffffffffffLL >> 1, 2, &a))) { goto ERROR; }
        if ((__builtin_smulll_overflow(
# 226 "builtin_overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL) 
# 226 "builtin_overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_smulll_overflow(0x7fffffffffffffffLL, 2, &a))) { goto ERROR; }
        if (!(__builtin_smulll_overflow(
# 228 "builtin_overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL)
# 228 "builtin_overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_smulll_overflow(0x7fffffffffffffffLL + 1uLL, 1, &a))) { goto ERROR; }
        if ((__builtin_smulll_overflow(1, 0x7fffffffffffffffLL + 1uLL, &a))) { goto ERROR; }
    }


    {
        unsigned int a;
        if ((__builtin_umul_overflow(
# 238 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 238 "builtin_overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_umul_overflow(
# 239 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U)
# 239 "builtin_overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_umul_overflow(
# 242 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 242 "builtin_overflow.c"
       + 1ull, 1, &a))) { goto ERROR; }
        if ((__builtin_umul_overflow(1, 
# 243 "builtin_overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 243 "builtin_overflow.c"
       + 1ull, &a))) { goto ERROR; }
    }


    {
        unsigned long a;
        if ((__builtin_umull_overflow(
# 249 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL) 
# 249 "builtin_overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_umull_overflow(
# 250 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL)
# 250 "builtin_overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_umull_overflow(
# 253 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL) 
# 253 "builtin_overflow.c"
       + 1ull, 1, &a))) { goto ERROR; }
        if ((__builtin_umull_overflow(1, 
# 254 "builtin_overflow.c" 3 4
       (0x7fffffffL * 2UL + 1UL) 
# 254 "builtin_overflow.c"
       + 1ull, &a))) { goto ERROR; }
    }


    {
        unsigned long long a;
        if ((__builtin_umulll_overflow(
# 260 "builtin_overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 260 "builtin_overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_umulll_overflow(
# 261 "builtin_overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL)
# 261 "builtin_overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_umulll_overflow(
# 264 "builtin_overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 264 "builtin_overflow.c"
       + 1uLL, 1, &a))) { goto ERROR; }
        if ((__builtin_umulll_overflow(1, 
# 265 "builtin_overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 265 "builtin_overflow.c"
       + 1uLL, &a))) { goto ERROR; }
    }

    return 0;

    ERROR:
    return 1;
}
