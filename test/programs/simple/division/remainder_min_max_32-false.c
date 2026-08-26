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

  short s_min = -32768;
  short s_max = 32767;

  int i_min = -2147483647 - 1;
  int i_max = 2147483647;

  long l_min = -2147483647L - 1L;
  long l_max = 2147483647L;

  long long ll_min = -9223372036854775807LL - 1LL;
  long long ll_max = 9223372036854775807LL;

  assert(!(ll_min % ll_max != -1 || l_min % l_max != -1 || i_min % i_max != -1 || s_min % s_max != -1 || sc_min % sc_max != -1));

  return 0;
}
