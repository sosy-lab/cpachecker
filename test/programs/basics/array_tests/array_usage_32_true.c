// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>
#include <stdlib.h>


// Safe for AssertionSafety only if all return the correct result
// This tests that an analysis can handle arrays (local and on the stack) in ILP32 (this fails for LP64)
int main() {
  
  char char_array[] = {25, 50, 75, 100};
  unsigned char uchar_array[] = {26, 51, 76, 101};

  short short_array[] = {27, 52, 77, 102};
  unsigned short ushort_array[] = {28, 53, 78, 103};

  int int_array[] = {29, 54, 79, 104};
  unsigned int uint_array[] = {30, 55, 80, 105};

  long long_array[] = {31, 56, 81, 106};
  unsigned long ulong_array[] = {32, 57, 82, 107};

  long long longlong_array[] = {33, 58, 83, 108};
  unsigned long long ulonglong_array[] = {34, 59, 84, 109};

  assert(char_array[0] == 25);
  assert(char_array[1] == 50);
  assert(char_array[2] == 75);
  assert(char_array[3] == 100);

  assert(uchar_array[0] == 26);
  assert(uchar_array[1] == 51);
  assert(uchar_array[2] == 76);
  assert(uchar_array[3] == 101);

  assert(short_array[0] == 27);
  assert(short_array[1] == 52);
  assert(short_array[2] == 77);
  assert(short_array[3] == 102);

  assert(ushort_array[0] == 28);
  assert(ushort_array[1] == 53);
  assert(ushort_array[2] == 78);
  assert(ushort_array[3] == 103);

  assert(int_array[0] == 29);
  assert(int_array[1] == 54);
  assert(int_array[2] == 79);
  assert(int_array[3] == 104);

  assert(uint_array[0] == 30);
  assert(uint_array[1] == 55);
  assert(uint_array[2] == 80);
  assert(uint_array[3] == 105);

  assert(long_array[0] == 31);
  assert(long_array[1] == 56);
  assert(long_array[2] == 81);
  assert(long_array[3] == 106);

  assert(ulong_array[0] == 32);
  assert(ulong_array[1] == 57);
  assert(ulong_array[2] == 82);
  assert(ulong_array[3] == 107);

  assert(longlong_array[0] == 33);
  assert(longlong_array[1] == 58);
  assert(longlong_array[2] == 83);
  assert(longlong_array[3] == 108);

  assert(ulonglong_array[0] == 34);
  assert(ulonglong_array[1] == 59);
  assert(ulonglong_array[2] == 84);
  assert(ulonglong_array[3] == 109);

  // Assign new values (w min and max of the types) and check again
  char_array[0] = -128;
  char_array[1] = -1;
  char_array[2] = 1;
  char_array[3] = 127;

  uchar_array[0] = 0;
  uchar_array[1] = 127;
  uchar_array[2] = 128;
  uchar_array[3] = 255;

  short_array[0] = -32768;
  short_array[1] = -129;
  short_array[2] = 128;
  short_array[3] = 32767;

  ushort_array[0] = 0;
  ushort_array[1] = 32767;
  ushort_array[2] = 32768;
  ushort_array[3] = 65535;

  int_array[0] = -2147483648;
  int_array[1] = -32769;
  int_array[2] = 32768;
  int_array[3] = 2147483647;

  uint_array[0] = 0;
  uint_array[1] = 2147483647u;
  uint_array[2] = 2147483649u;
  uint_array[3] = 4294967295u;

  long_array[0] = -2147483648l;
  long_array[1] = -32769l;
  long_array[2] = 32768l;
  long_array[3] = 2147483647l;

  ulong_array[0] = 0lu;
  ulong_array[1] = 2147483647lu;
  ulong_array[2] = 2147483649lu;
  ulong_array[3] = 4294967295lu;

  longlong_array[0] = -9223372036854775807ll;
  longlong_array[1] = -2147483650ll;
  longlong_array[2] = 2147483649ll;
  longlong_array[3] = 9223372036854775807ll;

  ulonglong_array[0] = 0;
  ulonglong_array[1] = 9223372036854775807llu;
  ulonglong_array[2] = 9223372036854775808llu;
  ulonglong_array[3] = 18446744073709551615llu;

  assert(char_array[0] == -128);
  assert(char_array[1] == -1);
  assert(char_array[2] == 1);
  assert(char_array[3] == 127);

  assert(uchar_array[0] == 0);
  assert(uchar_array[1] == 127);
  assert(uchar_array[2] == 128);
  assert(uchar_array[3] == 255);

  assert(short_array[0] == -32768);
  assert(short_array[1] == -129);
  assert(short_array[2] == 128);
  assert(short_array[3] == 32767);

  assert(ushort_array[0] == 0);
  assert(ushort_array[1] == 32767);
  assert(ushort_array[2] == 32768);
  assert(ushort_array[3] == 65535);

  assert(int_array[0] == -2147483648);
  assert(int_array[1] == -32769);
  assert(int_array[2] == 32768);
  assert(int_array[3] == 2147483647);

  assert(uint_array[0] == 0);
  assert(uint_array[1] == 2147483647);
  assert(uint_array[2] == 2147483649u);
  assert(uint_array[3] == 4294967295u);

  assert(long_array[0] == -2147483648l);
  assert(long_array[1] == -32769);
  assert(long_array[2] == 32768);
  assert(long_array[3] == 2147483647l);

  assert(ulong_array[0] == 0);
  assert(ulong_array[1] == 2147483647);
  assert(ulong_array[2] == 2147483649u);
  assert(ulong_array[3] == 4294967295u);

  assert(longlong_array[0] == -9223372036854775807ll);
  assert(longlong_array[1] == -2147483650ll);
  assert(longlong_array[2] == 2147483649);
  assert(longlong_array[3] == 9223372036854775807ll);

  assert(ulonglong_array[0] == 0);
  assert(ulonglong_array[1] == 9223372036854775807ll);
  assert(ulonglong_array[2] == 9223372036854775808llu);
  assert(ulonglong_array[3] == 18446744073709551615llu);

  // Test under/overflows
  uchar_array[3]++; // Overflow to min of type
  uchar_array[2]++;
  uchar_array[0]--; // Underflow to max of type
  uchar_array[1]--;
  assert(uchar_array[0] == 255);
  assert(uchar_array[1] == 126);
  assert(uchar_array[2] == 129);
  assert(uchar_array[3] == 0);

  uint_array[0]--;
  assert(uint_array[0] == 4294967295u);
  uint_array[0]--;
  assert(uint_array[0] == 4294967294u);

  ulong_array[0]--; // Underflow to max of type
  assert(ulong_array[0] == 4294967295u);

  ulong_array[3]++; // Overflow to min of type
  assert(ulong_array[3] == 0);

  ulonglong_array[3]++;
  assert(ulonglong_array[3] == 0);
  assert(ulonglong_array[2] == 9223372036854775808llu); // Assert that there is no change in [2]

  return 0;
}
