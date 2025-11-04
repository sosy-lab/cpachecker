// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  unsigned int a = 1;    // binary: 01
  unsigned int b = 2;    // binary: 10
  unsigned int c = 3;    // binary: 11

  if ((a && a) && (b && c)){
        // should be true
        return 0;
  }
  else{
        // error? should not happen
        return -1;
  }

}