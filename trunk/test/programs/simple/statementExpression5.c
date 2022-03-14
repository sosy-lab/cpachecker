// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

 // nested inside expression list, value used

int main()
{
  int a = 0;
  int x = ( // x == a - 1
      4,
      ({
        int y = ++a;
        (
          4,
          ({
            int y = ++a;
            (
              4,
              ({
                y = a++; // y == a - 1
              })
            );
          })
        );
      })
  );

  if(x != a - 1) {
    ERROR: // unreachable
      return 1;
  }
  
  return 0;
}
