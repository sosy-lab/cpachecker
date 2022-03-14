// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Created by Chengyu Zhang for #426

typedef unsigned int Size_t;
// The following alternative would make the program independent of the machine model
// typedef __SIZE_TYPE__ Size_t;

// Inlined:
// #define bufsize ((1L << (8 * sizeof(Size_t) - 2))-256)

struct huge_struct
{
  short buf[((1L << (8 * sizeof(Size_t) - 2))-256)];
  int a;
  int b;
  int c;
  int d;
};

union huge_union
{
  int a;
  char buf[((1L << (8 * sizeof(Size_t) - 2))-256)];
};

Size_t union_size()
{
  return sizeof(union huge_union);
}

Size_t struct_size()
{
  return sizeof(struct huge_struct);
}


int main()
{
  /* Check the exact sizeof value. bufsize is aligned on 256b. */
  if (union_size() != sizeof(char) * ((1L << (8 * sizeof(Size_t) - 2))-256))
    {ERROR:goto ERROR;}

  if (struct_size() != sizeof(short) * ((1L << (8 * sizeof(Size_t) - 2))-256) + 4*sizeof(int))
    goto ERROR;

  return 0;
}
