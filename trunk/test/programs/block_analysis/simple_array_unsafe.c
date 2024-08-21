// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {

  int x = 5;
  int arr[5] = {1, 2, 3, 4, 10};
  int sum = 0;
  for (int i = 0; i < x - 1; i++) {
    sum += arr[i];
  }
  if (arr[x - 1] == sum) {
    arr[x - 1] = 5;
    arr[x - 1] = arr[x - 1] + 2;
    goto ERROR;
  }
  return 0;
ERROR:
  return 1;
}
