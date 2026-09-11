// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

int main() {
  // Two struct typedefs, the second containing an instance of the first, both declared inside a
  // function body (as opposed to file scope): this shape used to send
  // CCompositeType.hasKnownConstantSize into infinite recursion (StackOverflowError) while
  // resolving the outer struct's size
  typedef struct {
    unsigned char data[4];
  } Inner;
  typedef struct {
    int tag;
    Inner in;
  } Outer;

  Outer o;
  o.tag = 0;
  o.in.data[2] = 42;
  if (o.in.data[2] != 42) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return -1;
}
