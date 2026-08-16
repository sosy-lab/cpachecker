// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  if (sizeof(_Bool) != 1) goto ERROR;
  if (sizeof(short) != 2) goto ERROR;
  if (sizeof(int) != 4) goto ERROR;
  if (sizeof(long) != 8) goto ERROR;
  if (sizeof(long long) != 8) goto ERROR;
  if (sizeof(float) != 4) goto ERROR;
  if (sizeof(double) != 8) goto ERROR;
  if (sizeof(long double) != 16) goto ERROR;
  if (sizeof(int*) != 8) goto ERROR;

  if (_Alignof(_Bool) != 1) goto ERROR;
  if (_Alignof(short) != 2) goto ERROR;
  if (_Alignof(int) != 4) goto ERROR;
  if (_Alignof(long) != 8) goto ERROR;
  if (_Alignof(long long) != 8) goto ERROR;
  if (_Alignof(float) != 4) goto ERROR;
  if (_Alignof(double) != 8) goto ERROR;
  if (_Alignof(long double) != 16) goto ERROR;
  if (_Alignof(int*) != 8) goto ERROR;

  if (sizeof(_Atomic _Bool) != 1) goto ERROR;
  if (sizeof(_Atomic short) != 2) goto ERROR;
  if (sizeof(_Atomic int) != 4) goto ERROR;
  if (sizeof(_Atomic long) != 8) goto ERROR;
  if (sizeof(_Atomic long long) != 8) goto ERROR;
  if (sizeof(_Atomic float) != 4) goto ERROR;
  if (sizeof(_Atomic double) != 8) goto ERROR;
  if (sizeof(_Atomic long double) != 16) goto ERROR;
  if (sizeof(_Atomic int*) != 8) goto ERROR;

  if (_Alignof(_Atomic _Bool) != 1) goto ERROR;
  if (_Alignof(_Atomic short) != 2) goto ERROR;
  if (_Alignof(_Atomic int) != 4) goto ERROR;
  if (_Alignof(_Atomic long) != 8) goto ERROR;
  if (_Alignof(_Atomic long long) != 8) goto ERROR;
  if (_Alignof(_Atomic float) != 4) goto ERROR;
  if (_Alignof(_Atomic double) != 8) goto ERROR;
  if (_Alignof(_Atomic long double) != 16) goto ERROR;
  if (_Alignof(_Atomic int*) != 8) goto ERROR;

  return 0;
ERROR:
  return 1;
}
