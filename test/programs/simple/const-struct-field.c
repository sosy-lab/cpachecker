// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct F { int x;};
int main()
{
  int timeout = 0;
  int x = 0;
  while (1)
    {
      const struct F i = { x++};
      if (i.x > 0)
break;
      if (++timeout > 5)
{ERROR:goto ERROR;}
    }
  return 0;
}
