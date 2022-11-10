// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>

int main() {
  int arrayInitList[] = {1,2,3,4,5,6,7,8,9,10};
  int arrayOneInit[] = {1};
  int arraySizeLargerInit[3] = {1};
  int length = 5;
  int arrayFromConcreteVariable[5];
  int arrayFromConcreteVariableAndInitList[5] = {1,2,3,4,5};
  int arrGccRangeFixedSize[9] = { [0 ... 8] = 10 };
  int arrGccRangeFixedRangeExtented[10] = { [0 ... 8] = 10 , 11};
  int arrGccRangeRangeByInit[] = { [0 ... 8] = 10 };
  int arrGccRangeByInitExtented[] = { [0 ... 8] = 10 , 11};
  int arrRange[9] = { [5] = 10 };
  int arrRangeExtented[] = { [8] = 10 , 11};
}
