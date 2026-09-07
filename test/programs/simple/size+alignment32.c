// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// An atomic struct type may need stricter alignment than its non-atomic version, just like an
// atomic scalar type: if the size of the struct is one of 1, 2, 4, 8, or 16 bytes and the
// alignment of the non-atomic struct is smaller than that size, the atomic struct is aligned to
// its size. Cf. https://gcc.gnu.org/wiki/Atomic/GCCMM/UnalignedPolicy and
// https://docs.oracle.com/cd/E60778_01/html/E60745/gqfbq.html.
struct pair {
  int a;
  int b;
};

int main() {
  if (sizeof(_Bool) != 1) goto ERROR;
  if (sizeof(short) != 2) goto ERROR;
  if (sizeof(int) != 4) goto ERROR;
  if (sizeof(long) != 4) goto ERROR;
  if (sizeof(long long) != 8) goto ERROR;
  if (sizeof(float) != 4) goto ERROR;
  if (sizeof(double) != 8) goto ERROR;
  if (sizeof(long double) != 12) goto ERROR;
  if (sizeof(int*) != 4) goto ERROR;

  if (_Alignof(_Bool) != 1) goto ERROR;
  if (_Alignof(short) != 2) goto ERROR;
  if (_Alignof(int) != 4) goto ERROR;
  if (_Alignof(long) != 4) goto ERROR;
  if (_Alignof(long long) != 4) goto ERROR;
  if (_Alignof(float) != 4) goto ERROR;
  if (_Alignof(double) != 4) goto ERROR;
  if (_Alignof(long double) != 4) goto ERROR;
  if (_Alignof(int*) != 4) goto ERROR;

  if (sizeof(_Atomic _Bool) != 1) goto ERROR;
  if (sizeof(_Atomic short) != 2) goto ERROR;
  if (sizeof(_Atomic int) != 4) goto ERROR;
  if (sizeof(_Atomic long) != 4) goto ERROR;
  if (sizeof(_Atomic long long) != 8) goto ERROR;
  if (sizeof(_Atomic float) != 4) goto ERROR;
  if (sizeof(_Atomic double) != 8) goto ERROR;
  if (sizeof(_Atomic long double) != 12) goto ERROR;
  if (sizeof(_Atomic int*) != 4) goto ERROR;

  if (_Alignof(_Atomic _Bool) != 1) goto ERROR;
  if (_Alignof(_Atomic short) != 2) goto ERROR;
  if (_Alignof(_Atomic int) != 4) goto ERROR;
  if (_Alignof(_Atomic long) != 4) goto ERROR;
  if (_Alignof(_Atomic long long) != 8) goto ERROR;
  if (_Alignof(_Atomic float) != 4) goto ERROR;
  if (_Alignof(_Atomic double) != 8) goto ERROR;
  if (_Alignof(_Atomic long double) != 4) goto ERROR;
  if (_Alignof(_Atomic int*) != 4) goto ERROR;

  if (sizeof(struct pair) != 8) goto ERROR;
  if (_Alignof(struct pair) != 4) goto ERROR;
  if (sizeof(_Atomic struct pair) != 8) goto ERROR;
  if (_Alignof(_Atomic struct pair) != 8) goto ERROR;

  return 0;
ERROR:
  return 1;
}
