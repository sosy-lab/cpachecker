// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int a = 1;    // binary: 01
  int b = 2;    // binary: 10

  int bitwise_and = a & b;
  int logical_and = a && b;

  return (bitwise_and != logical_and);
}