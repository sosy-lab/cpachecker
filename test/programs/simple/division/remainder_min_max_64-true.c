// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


int main() {

  signed char sc_min = -128;
  signed char sc_max = 127;

  assert(sc_min % sc_max == -1);


  short s_min = -32768;
  short s_max = 32767;

  assert(s_min % s_max == -1);


  int i_min = -2147483647 - 1;
  int i_max = 2147483647;

  assert(i_min % i_max == -1);


  long l_min = -9223372036854775807L - 1L;
  long l_max = 9223372036854775807L;

  assert(l_min % l_max == -1);


  long long ll_min = -9223372036854775807LL - 1LL;
  long long ll_max = 9223372036854775807LL;

  assert(ll_min % ll_max == -1);

  return 0;
}
