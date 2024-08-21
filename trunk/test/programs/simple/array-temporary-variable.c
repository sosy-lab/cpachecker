// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned long size_t;

union my_union {
   char const   *nameptr ;
   char name[81] ;
};

struct my_struct {
   size_t my_size ;
   union my_union my_name ;
};

void f(int * p) {
}

int main(struct my_struct const   *attr) {
  char const *name ;

  name = attr->my_size < (size_t const)sizeof(*attr) ? attr->my_name.nameptr : attr->my_name.name;
  if (name == 0) {
    return 0;
  }

  f((int[]) {1, 2, 3});

ERROR:
  return 1;
}
