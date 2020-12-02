// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main() {
  {
	long a = 1;
	unsigned b = 1U;
	long c = 1L;
	unsigned long d = 1LU;
	unsigned long e = 1UL;
	long long f = 1LL;
	unsigned long long g = 1LLU;
	unsigned long long h = 1ULL;
  }
  {
    long a = -1;
	unsigned b = -1U;
	long c = -1L;
	unsigned long d = -1LU;
	unsigned long e = -1UL;
	long long f = -1LL;
	unsigned long long g = -1LLU;
	unsigned long long h = -1ULL;
  }
  {
	long a = 0;
	unsigned b = 0U;
	long c = 0L;
	unsigned long d = 0LU;
	unsigned long e = 0UL;
	long long f = 0LL;
	unsigned long long g = 0LLU;
	unsigned long long h = 0ULL;
  }
  {
	long a = ~0;
	unsigned b = ~0U;
	long c = ~0L;
	unsigned long d = ~0LU;
	unsigned long e = ~0UL;
	long long f = ~0LL;
	unsigned long long g = ~0LLU;
	unsigned long long h = ~0ULL;
  }

  {
	long a = 1ULL;
	unsigned b = 1ULL;
	long c = 1ULL;
	unsigned long d = 1ULL;
	unsigned long e = 1ULL;
	long long f = 1ULL;
	unsigned long long g = 1ULL;
	unsigned long long h = 1ULL;
  }
  {
    long a = -1ULL;
	unsigned b = -1ULL;
	long c = -1ULL;
	unsigned long d = -1ULL;
	unsigned long e = -1ULL;
	long long f = -1ULL;
	unsigned long long g = -1ULL;
	unsigned long long h = -1ULL;
  }
  {
	long a = 0ULL;
	unsigned b = 0ULL;
	long c = 0ULL;
	unsigned long d = 0ULL;
	unsigned long e = 0ULL;
	long long f = 0ULL;
	unsigned long long g = 0ULL;
	unsigned long long h = 0ULL;
  }
  {
	long a = ~0ULL;
	unsigned b = ~0ULL;
	long c = ~0ULL;
	unsigned long d = ~0ULL;
	unsigned long e = ~0ULL;
	long long f = ~0ULL;
	unsigned long long g = ~0ULL;
	unsigned long long h = ~0ULL;
  }
}
