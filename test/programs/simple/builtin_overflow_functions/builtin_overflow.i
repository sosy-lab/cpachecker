// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

# 1 "overflow.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 31 "<command-line>"
# 1 "/usr/include/stdc-predef.h" 1 3 4
# 32 "<command-line>" 2
# 1 "overflow.c"
# 9 "overflow.c"
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
# 10 "overflow.c" 2
# 24 "overflow.c"
int main()
{

    {

        int a; long long c;
        if (!(__builtin_add_overflow(0x7fffffff + 1l, 0x7fffffff, &a))) { goto ERROR; }
        if ((__builtin_add_overflow(0x7fffffff + 1l, -100, &a))) { goto ERROR; }
        if ((__builtin_add_overflow(0x7fffffff, 1, &c))) { goto ERROR; }

        int x = 0x7fffffff, y = 1;
        if ((__builtin_add_overflow(x, y, &c))) { goto ERROR; }
        x = 0x7fffffff, y = 0x7fffffff;
        if (!(__builtin_add_overflow(x, y, &a))) { goto ERROR; }
    }


    {
        int a;
        if ((__builtin_sadd_overflow(0x7fffffff - 1, 1, &a))) { goto ERROR; }
        if ((__builtin_sadd_overflow(
# 44 "overflow.c" 3 4
       (-0x7fffffff - 1) 
# 44 "overflow.c"
       + 1, -1, &a))) { goto ERROR; }
        if (!(__builtin_sadd_overflow(0x7fffffff, 1, &a))) { goto ERROR; }
        if (!(__builtin_sadd_overflow(
# 46 "overflow.c" 3 4
       (-0x7fffffff - 1)
# 46 "overflow.c"
       , -1, &a))) { goto ERROR; }

        long long int c;
        if (!(__builtin_sadd_overflow(0x7fffffff, 1, &c))) { goto ERROR; }


        if ((__builtin_sadd_overflow(0x7fffffff + 1l, 0x7fffffff, &a))) { goto ERROR; }
        if ((__builtin_sadd_overflow(
# 53 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U)
# 53 "overflow.c"
       , 0x7fffffff, &a))) { goto ERROR; }
        if (!(__builtin_sadd_overflow(0x7fffffff + 1l, -100, &a))) { goto ERROR; }

    }


    {
        long a;
        if ((__builtin_saddl_overflow(0x7fffffffffffffffL - 1l, 1l, &a))) { goto ERROR; }
        if ((__builtin_saddl_overflow(
# 62 "overflow.c" 3 4
       (-0x7fffffffffffffffL - 1L) 
# 62 "overflow.c"
       + 1l, -1l, &a))) { goto ERROR; }
        if (!(__builtin_saddl_overflow(0x7fffffffffffffffL, 1l, &a))) { goto ERROR; }
        if (!(__builtin_saddl_overflow(
# 64 "overflow.c" 3 4
       (-0x7fffffffffffffffL - 1L)
# 64 "overflow.c"
       , -1l, &a))) { goto ERROR; }

        int c;
        if ((__builtin_saddl_overflow(0x7fffffff, 1l, &c))) { goto ERROR; }


        if ((__builtin_saddl_overflow(0x7fffffffffffffffL + 1LL, 0x7fffffffffffffffL, &a))) { goto ERROR; }
        if ((__builtin_saddl_overflow(
# 71 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL)
# 71 "overflow.c"
       , 0x7fffffffffffffffL, &a))) { goto ERROR; }
        if (!(__builtin_saddl_overflow(0x7fffffffffffffffL + 1LL, -100l, &a))) { goto ERROR; }
    }


    {
        long long a;
        if ((__builtin_saddll_overflow(0x7fffffffffffffffLL - 1LL, 1LL, &a))) { goto ERROR; }
        if ((__builtin_saddll_overflow(
# 79 "overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL) 
# 79 "overflow.c"
       + 1LL, -1LL, &a))) { goto ERROR; }
        if (!(__builtin_saddll_overflow(0x7fffffffffffffffLL, 1LL, &a))) { goto ERROR; }
        if (!(__builtin_saddll_overflow(
# 81 "overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL)
# 81 "overflow.c"
       , -1LL, &a))) { goto ERROR; }
    }


    {
        unsigned int a;
        if ((__builtin_uadd_overflow(
# 87 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 87 "overflow.c"
       - 1u, 1u, &a))) { goto ERROR; }
        if (!(__builtin_uadd_overflow(
# 88 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U)
# 88 "overflow.c"
       , 1u, &a))) { goto ERROR; }


        if ((__builtin_uadd_overflow(
# 91 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 91 "overflow.c"
       + 1ul, 1u, &a))) { goto ERROR; }
        if (!(__builtin_uadd_overflow(-0x7fffffff, 0x7fffffff, &a))) { goto ERROR; }
    }


    {
        unsigned long a;
        if ((__builtin_uaddl_overflow(
# 98 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 98 "overflow.c"
       - 1ul, 1ul, &a))) { goto ERROR; }
        if (!(__builtin_uaddl_overflow(
# 99 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL)
# 99 "overflow.c"
       , 1ul, &a))) { goto ERROR; }


        if ((__builtin_uadd_overflow(
# 102 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 102 "overflow.c"
       + 1uLL, 1ul, &a))) { goto ERROR; }
        if (!(__builtin_uadd_overflow(-0x7fffffffffffffffL, 0x7fffffffffffffffL, &a))) { goto ERROR; }
    }



    {
        unsigned long long a;
        if ((__builtin_uaddll_overflow(
# 110 "overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 110 "overflow.c"
       - 1uLL, 1uLL, &a))) { goto ERROR; }
        if (!(__builtin_uaddll_overflow(
# 111 "overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL)
# 111 "overflow.c"
       , 1uLL, &a))) { goto ERROR; }
    }


    {

        int a; long long c;
        if (!(__builtin_sub_overflow(
# 118 "overflow.c" 3 4
       (-0x7fffffff - 1) 
# 118 "overflow.c"
       - 1l, 0x7fffffff, &a))) { goto ERROR; }
        if ((__builtin_sub_overflow(
# 119 "overflow.c" 3 4
       (-0x7fffffff - 1) 
# 119 "overflow.c"
       - 1l, -100, &a))) { goto ERROR; }
        if ((__builtin_sub_overflow(
# 120 "overflow.c" 3 4
       (-0x7fffffff - 1)
# 120 "overflow.c"
       , 1, &c))) { goto ERROR; }
    }


    {
        int a;
        if ((__builtin_ssub_overflow(0x7fffffff - 1, -1, &a))) { goto ERROR; }
        if ((__builtin_ssub_overflow(
# 127 "overflow.c" 3 4
       (-0x7fffffff - 1) 
# 127 "overflow.c"
       + 1, 1, &a))) { goto ERROR; }
        if (!(__builtin_ssub_overflow(0x7fffffff, -1, &a))) { goto ERROR; }
        if (!(__builtin_ssub_overflow(
# 129 "overflow.c" 3 4
       (-0x7fffffff - 1)
# 129 "overflow.c"
       , 1, &a))) { goto ERROR; }


        if ((__builtin_ssub_overflow(0x7fffffff + 1l, 
# 132 "overflow.c" 3 4
       (-0x7fffffff - 1)
# 132 "overflow.c"
       , &a))) { goto ERROR; }
        if ((__builtin_ssub_overflow(
# 133 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U)
# 133 "overflow.c"
       , 
# 133 "overflow.c" 3 4
       (-0x7fffffff - 1)
# 133 "overflow.c"
       , &a))) { goto ERROR; }
        if (!(__builtin_ssub_overflow(0x7fffffff + 1l, 100, &a))) { goto ERROR; }
    }


    {
        long a;
        if ((__builtin_ssubl_overflow(0x7fffffffffffffffL - 1l, -1l, &a))) { goto ERROR; }
        if ((__builtin_ssubl_overflow(
# 141 "overflow.c" 3 4
       (-0x7fffffffffffffffL - 1L) 
# 141 "overflow.c"
       + 1l, 1l, &a))) { goto ERROR; }
        if (!(__builtin_ssubl_overflow(0x7fffffffffffffffL, -1l, &a))) { goto ERROR; }
        if (!(__builtin_ssubl_overflow(
# 143 "overflow.c" 3 4
       (-0x7fffffffffffffffL - 1L)
# 143 "overflow.c"
       , 1l, &a))) { goto ERROR; }


        if ((__builtin_ssubl_overflow(0x7fffffffffffffffL + 1LL, 
# 146 "overflow.c" 3 4
       (-0x7fffffffffffffffL - 1L)
# 146 "overflow.c"
       , &a))) { goto ERROR; }
        if ((__builtin_ssubl_overflow(
# 147 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL)
# 147 "overflow.c"
       , 
# 147 "overflow.c" 3 4
       (-0x7fffffffffffffffL - 1L)
# 147 "overflow.c"
       , &a))) { goto ERROR; }
        if (!(__builtin_ssubl_overflow(0x7fffffffffffffffL + 1LL, 100l, &a))) { goto ERROR; }
    }


    {
        long long a;
        if ((__builtin_ssubll_overflow(0x7fffffffffffffffLL - 1LL, -1LL, &a))) { goto ERROR; }
        if ((__builtin_ssubll_overflow(
# 155 "overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL) 
# 155 "overflow.c"
       + 1LL, 1LL, &a))) { goto ERROR; }
        if (!(__builtin_ssubll_overflow(0x7fffffffffffffffLL, -1LL, &a))) { goto ERROR; }
        if (!(__builtin_ssubll_overflow(
# 157 "overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL)
# 157 "overflow.c"
       , 1LL, &a))) { goto ERROR; }
    }


    {
        unsigned int a;
        if ((__builtin_usub_overflow(1u, 1u, &a))) { goto ERROR; }
        if (!(__builtin_usub_overflow(0u, 1u, &a))) { goto ERROR; }


        if (!(__builtin_usub_overflow(
# 167 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 167 "overflow.c"
       + 1ul, 1u, &a))) { goto ERROR; }
    }


    {
        unsigned long int a;
        if ((__builtin_usubl_overflow(1ul, 1ul, &a))) { goto ERROR; }
        if (!(__builtin_usubl_overflow(0ul, 1ul, &a))) { goto ERROR; }


        if (!(__builtin_usub_overflow(
# 177 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 177 "overflow.c"
       + 1uLL, 1ul, &a))) { goto ERROR; }
    }


    {
        unsigned long long int a;
        if ((__builtin_usubll_overflow(1uLL, 1uLL, &a))) { goto ERROR; }
        if (!(__builtin_usubll_overflow(0uLL, 1uLL, &a))) { goto ERROR; }
    }


    {
        int a; long long c;
        if (!(__builtin_mul_overflow(0x7fffffff + 1l, 1, &a))) { goto ERROR; }
        if (!(__builtin_mul_overflow(
# 191 "overflow.c" 3 4
       (-0x7fffffff - 1) 
# 191 "overflow.c"
       - 1l, -1, &a))) { goto ERROR; }
        if ((__builtin_mul_overflow(0x7fffffff, 2, &c))) { goto ERROR; }
    }


    {
        int a;
        if ((__builtin_smul_overflow(0x7fffffff >> 1, 2, &a))) { goto ERROR; }
        if ((__builtin_smul_overflow(
# 199 "overflow.c" 3 4
       (-0x7fffffff - 1) 
# 199 "overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_smul_overflow(0x7fffffff, 2, &a))) { goto ERROR; }
        if (!(__builtin_smul_overflow(
# 201 "overflow.c" 3 4
       (-0x7fffffff - 1)
# 201 "overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_smul_overflow(0x7fffffff + 1l, 1, &a))) { goto ERROR; }
        if ((__builtin_smul_overflow(1, 0x7fffffff + 1l, &a))) { goto ERROR; }
    }


    {
        long a;
        if ((__builtin_smull_overflow(0x7fffffffffffffffL >> 1, 2, &a))) { goto ERROR; }
        if ((__builtin_smull_overflow(
# 212 "overflow.c" 3 4
       (-0x7fffffffffffffffL - 1L) 
# 212 "overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_smull_overflow(0x7fffffffffffffffL, 2, &a))) { goto ERROR; }
        if (!(__builtin_smull_overflow(
# 214 "overflow.c" 3 4
       (-0x7fffffffffffffffL - 1L)
# 214 "overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_smull_overflow(0x7fffffffffffffffL + 1ll, 1, &a))) { goto ERROR; }
        if ((__builtin_smull_overflow(1, 0x7fffffffffffffffL + 1ll, &a))) { goto ERROR; }
    }


    {
        long long a;
        if ((__builtin_smulll_overflow(0x7fffffffffffffffLL >> 1, 2, &a))) { goto ERROR; }
        if ((__builtin_smulll_overflow(
# 225 "overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL) 
# 225 "overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_smulll_overflow(0x7fffffffffffffffLL, 2, &a))) { goto ERROR; }
        if (!(__builtin_smulll_overflow(
# 227 "overflow.c" 3 4
       (-0x7fffffffffffffffLL - 1LL)
# 227 "overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_smulll_overflow(0x7fffffffffffffffLL + 1uLL, 1, &a))) { goto ERROR; }
        if ((__builtin_smulll_overflow(1, 0x7fffffffffffffffLL + 1uLL, &a))) { goto ERROR; }
    }


    {
        unsigned int a;
        if ((__builtin_umul_overflow(
# 237 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 237 "overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_umul_overflow(
# 238 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U)
# 238 "overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_umul_overflow(
# 241 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 241 "overflow.c"
       + 1ul, 1, &a))) { goto ERROR; }
        if ((__builtin_umul_overflow(1, 
# 242 "overflow.c" 3 4
       (0x7fffffff * 2U + 1U) 
# 242 "overflow.c"
       + 1ul, &a))) { goto ERROR; }
    }


    {
        unsigned long a;
        if ((__builtin_umull_overflow(
# 248 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 248 "overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_umull_overflow(
# 249 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL)
# 249 "overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_umull_overflow(
# 252 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 252 "overflow.c"
       + 1ull, 1, &a))) { goto ERROR; }
        if ((__builtin_umull_overflow(1, 
# 253 "overflow.c" 3 4
       (0x7fffffffffffffffL * 2UL + 1UL) 
# 253 "overflow.c"
       + 1ull, &a))) { goto ERROR; }
    }


    {
        unsigned long long a;
        if ((__builtin_umulll_overflow(
# 259 "overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 259 "overflow.c"
       >> 1, 2, &a))) { goto ERROR; }
        if (!(__builtin_umulll_overflow(
# 260 "overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL)
# 260 "overflow.c"
       , 2, &a))) { goto ERROR; }


        if ((__builtin_umulll_overflow(
# 263 "overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 263 "overflow.c"
       + 1uLL, 1, &a))) { goto ERROR; }
        if ((__builtin_umulll_overflow(1, 
# 264 "overflow.c" 3 4
       (0x7fffffffffffffffLL * 2ULL + 1ULL) 
# 264 "overflow.c"
       + 1uLL, &a))) { goto ERROR; }
    }

    return 0;

    ERROR:
    return 1;
}
