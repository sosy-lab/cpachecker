// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
int main() {
  int a[100000];

  for (int j = 0; j < 100000; j++) {
    a[j] = j;
  }

  int swapped = 1;
  while (swapped) {
    swapped = 0;
    int i = 1;
    while (i < 100000) {
      if (a[i - 1] > a[i]) {
        int t = a[i];
        a[i] = a[i - 1];
        a[i-1] = t;
        swapped = 1;
      }
      i = i + 1;
    }
  }

  for (int x = 0; x < 100000; x++) {
    for (int y = x + 1; y < 100000; y++) {
      if (a[x] < a[y]) {
        goto ERROR;
      }
    }
  }
  goto SUCCESS;

  SUCCESS:
    return 0;
  ERROR:
    return -1;
}

int swap(int a[], int i) {
  int t = a[i];
  a[i] = a[i - 1];
  a[i-1] = t;
  return 0;
}