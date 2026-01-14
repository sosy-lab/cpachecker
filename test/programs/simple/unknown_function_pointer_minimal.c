// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error(){}
typedef unsigned long size_t;

struct allocator {
   void (*die) (size_t);
};

struct allocator const stdlib_allocator = {(void (*)(size_t ))((void *)0)};

int main () {
  { if(!(stdlib_allocator.die == 0)) { reach_error(); } };
  reach_error();
}