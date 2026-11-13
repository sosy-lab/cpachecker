// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Constant conditions in global initializers with unary operators on
// typedef-typed operands (gnulib's TYPE_MINIMUM in coreutils' mktime.c).

extern void reach_error(void);

typedef long time_t;
typedef long long_int;
typedef int myint;
typedef double dbl;

static const long_int mktime_min =
    (!((time_t)0 < (time_t)-1))
            && ((time_t)~(time_t)((((time_t)1 << (sizeof(time_t) * 8 - 2)) - 1) * 2 + 1)
                < (long_int)~(long_int)((((long_int)1 << (sizeof(long_int) * 8 - 2)) - 1) * 2 + 1))
        ? (long_int)~(long_int)((((long_int)1 << (sizeof(long_int) * 8 - 2)) - 1) * 2 + 1)
        : (time_t)~(time_t)((((time_t)1 << (sizeof(time_t) * 8 - 2)) - 1) * 2 + 1);

static const myint neg = -(myint)5 < 0 ? -(myint)5 : 0;

static const int negDouble = -(dbl)1 < 0 ? 1 : 0;

int main(void) {
  if (mktime_min >= 0 || ~mktime_min != (((time_t)1 << (sizeof(time_t) * 8 - 2)) - 1) * 2 + 1) {
    reach_error();
  }
  if (neg != -5 || negDouble != 1) {
    reach_error();
  }
  return 0;
}
