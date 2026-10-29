// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned __VERIFIER_nondet_uint();

int main()
{
  int b = __VERIFIER_nondet_uint();
  int* a = &b;
  
  if (*a != 1) 
  {
    *a = 0;
  } 

  if (*a>0) 
  {
    *a = *a -1;
  }
  
  return *a;
}

