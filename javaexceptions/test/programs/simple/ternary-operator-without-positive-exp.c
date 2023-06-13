// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = ( ({
    int __val;
    {
      if (({
        1;
      } )
      == 1)
        __val = 1U;
      switch (4UL)
      {
        case 4:;
        goto ldv_35185;
        default:;
        ldv_35190:;
        goto ldv_35190;
      }
      ldv_35185:;
    }
    __val;
  } ) )
  ? : 1;
  if (x != 1) {
ERROR:
	return 1;
  }
  return 0;
}
