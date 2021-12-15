// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long long __s64;
typedef __s64 time64_t;
typedef __s64 s64;

struct timespec64 {
   time64_t tv_sec ;
   long tv_nsec ;
};

struct timespec64 ns_to_timespec64(s64 time) {
  struct timespec64 res;
  res.tv_nsec = 1;
  res.tv_sec = time;
  return res;
}

static int ptp_dte_gettime(struct timespec64 *ts)
{
  s64 test;
  test = 10;
  *ts = ns_to_timespec64(test);
  return 0;
}

int main(void)
{
  struct timespec64 ts;
  ptp_dte_gettime(&ts);
  if (ts.tv_nsec != 1 || ts.tv_sec != 10) {
    char tmp = *(char *)0;
  }
  return 0;
}
