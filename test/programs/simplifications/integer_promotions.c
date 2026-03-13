// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  unsigned char uc1 = 200;
  unsigned char uc2 = 100;
  signed char sc1 = 100;
  signed char sc2 = 27;

  if ((uc1 + uc2) != 300) goto ERROR;
  if ((sc1 + sc2) != 127) goto ERROR;
  if (((unsigned char)250 + (unsigned char)10) != 260) goto ERROR;
  if (((short)30000 + (short)2000) != 32000) goto ERROR;

  return 0;
ERROR:
  return 1;
}
