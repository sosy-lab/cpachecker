// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

void reach_error() {}
extern void *memset(void *s, int c, size_t n);

struct rec {
  int tag;
  char data[4];
};

int main() {
  struct rec arr[2];
  arr[1].data[2] = 'X';
  // sizeof(struct rec) == 8 (int tag = 4 bytes + char data[4] = 4 bytes), no padding.
  // 9 bytes = 1 full element (arr[0]) plus 1 remainder byte, which only reaches the first byte
  // of arr[1].tag. arr[1].data (bytes 12..15) is not within the 9-byte range at all.
  memset(arr, 0, 9);
  if (arr[1].data[2] == 'X') {
    // reachable: the memset never reaches arr[1].data, so 'X' survives.
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return -1;
}
