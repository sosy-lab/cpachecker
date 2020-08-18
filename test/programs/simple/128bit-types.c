// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  __int128 i128;
  unsigned __int128 ui128;
  __int128_t i128_t;
  __uint128_t ui128_t;
  __float128 f128;

  const int i = 16;

  if (sizeof(i128) != i) goto EXIT;
  if (sizeof(ui128) != i) goto EXIT;
  if (sizeof(i128_t) != i) goto EXIT;
  if (sizeof(ui128_t) != i) goto EXIT;
  if (sizeof(f128) != i) goto EXIT;

  if (_Alignof(i128) != i) goto EXIT;
  if (_Alignof(ui128) != i) goto EXIT;
  if (_Alignof(i128_t) != i) goto EXIT;
  if (_Alignof(ui128_t) != i) goto EXIT;
  if (_Alignof(f128) != i) goto EXIT;

  if (((__int128)2 << 100) <= 0) goto EXIT;

ERROR:
  return 1;

EXIT:
  return 0;
}

