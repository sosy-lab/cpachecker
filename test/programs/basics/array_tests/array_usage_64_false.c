// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdlib.h>


// Unsafe for ReachSafety only if all ifs are not fulfilled
// This tests that an analysis can handle arrays (local and on the stack) in LP64
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

  if(char_array[0] != 25) {
    goto NO_ERROR;
  }
  if(char_array[1] != 50) {
    goto NO_ERROR;
  }
  if(char_array[2] != 75) {
    goto NO_ERROR;
  }
  if(char_array[3] != 100) {
    goto NO_ERROR;
  }

  if(uchar_array[0] != 26) {
    goto NO_ERROR;
  }
  if(uchar_array[1] != 51) {
    goto NO_ERROR;
  }
  if(uchar_array[2] != 76) {
    goto NO_ERROR;
  }
  if(uchar_array[3] != 101) {
    goto NO_ERROR;
  }

  if(short_array[0] != 27) {
    goto NO_ERROR;
  }
  if(short_array[1] != 52) {
    goto NO_ERROR;
  }
  if(short_array[2] != 77) {
    goto NO_ERROR;
  }
  if(short_array[3] != 102) {
    goto NO_ERROR;
  }

  if(ushort_array[0] != 28) {
    goto NO_ERROR;
  }
  if(ushort_array[1] != 53) {
    goto NO_ERROR;
  }
  if(ushort_array[2] != 78) {
    goto NO_ERROR;
  }
  if(ushort_array[3] != 103) {
    goto NO_ERROR;
  }

  if(int_array[0] != 29) {
    goto NO_ERROR;
  }
  if(int_array[1] != 54) {
    goto NO_ERROR;
  }
  if(int_array[2] != 79) {
    goto NO_ERROR;
  }
  if(int_array[3] != 104) {
    goto NO_ERROR;
  }

  if(uint_array[0] != 30) {
    goto NO_ERROR;
  }
  if(uint_array[1] != 55) {
    goto NO_ERROR;
  }
  if(uint_array[2] != 80) {
    goto NO_ERROR;
  }
  if(uint_array[3] != 105) {
    goto NO_ERROR;
  }

  if(long_array[0] != 31) {
    goto NO_ERROR;
  }
  if(long_array[1] != 56) {
    goto NO_ERROR;
  }
  if(long_array[2] != 81) {
    goto NO_ERROR;
  }
  if(long_array[3] != 106) {
    goto NO_ERROR;
  }

  if(ulong_array[0] != 32) {
    goto NO_ERROR;
  }
  if(ulong_array[1] != 57) {
    goto NO_ERROR;
  }
  if(ulong_array[2] != 82) {
    goto NO_ERROR;
  }
  if(ulong_array[3] != 107) {
    goto NO_ERROR;
  }

  if(longlong_array[0] != 33) {
    goto NO_ERROR;
  }
  if(longlong_array[1] != 58) {
    goto NO_ERROR;
  }
  if(longlong_array[2] != 83) {
    goto NO_ERROR;
  }
  if(longlong_array[3] != 108) {
    goto NO_ERROR;
  }

  if(ulonglong_array[0] != 34) {
    goto NO_ERROR;
  }
  if(ulonglong_array[1] != 59) {
    goto NO_ERROR;
  }
  if(ulonglong_array[2] != 84) {
    goto NO_ERROR;
  }
  if(ulonglong_array[3] != 109) {
    goto NO_ERROR;
  }


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

  long_array[0] = -9223372036854775807l;
  long_array[1] = -2147483650l;
  long_array[2] = 2147483649l;
  long_array[3] = 9223372036854775807l;

  ulong_array[0] = 0lu;
  ulong_array[1] = 9223372036854775807lu;
  ulong_array[2] = 9223372036854775808lu;
  ulong_array[3] = 18446744073709551615lu;

  longlong_array[0] = -9223372036854775807ll;
  longlong_array[1] = -2147483650ll;
  longlong_array[2] = 2147483649ll;
  longlong_array[3] = 9223372036854775807ll;

  ulonglong_array[0] = 0;
  ulonglong_array[1] = 9223372036854775807llu;
  ulonglong_array[2] = 9223372036854775808llu;
  ulonglong_array[3] = 18446744073709551615llu;

  if (char_array[0] != -128) {
    goto NO_ERROR;
  }
  if (char_array[1] != -1) {
    goto NO_ERROR;
  }
  if (char_array[2] != 1) {
    goto NO_ERROR;
  }
  if (char_array[3] != 127) {
    goto NO_ERROR;
  }

  if (uchar_array[0] != 0) {
    goto NO_ERROR;
  }
  if (uchar_array[1] != 127) {
    goto NO_ERROR;
  }
  if (uchar_array[2] != 128) {
    goto NO_ERROR;
  }
  if (uchar_array[3] != 255) {
    goto NO_ERROR;
  }

  if (short_array[0] != -32768) {
    goto NO_ERROR;
  }
  if (short_array[1] != -129) {
    goto NO_ERROR;
  }
  if (short_array[2] != 128) {
    goto NO_ERROR;
  }
  if (short_array[3] != 32767) {
    goto NO_ERROR;
  }

  if (ushort_array[0] != 0) {
    goto NO_ERROR;
  }
  if (ushort_array[1] != 32767) {
    goto NO_ERROR;
  }
  if (ushort_array[2] != 32768) {
    goto NO_ERROR;
  }
  if (ushort_array[3] != 65535) {
    goto NO_ERROR;
  }

  if (int_array[0] != -2147483648) {
    goto NO_ERROR;
  }
  if (int_array[1] != -32769) {
    goto NO_ERROR;
  }
  if (int_array[2] != 32768) {
    goto NO_ERROR;
  }
  if (int_array[3] != 2147483647) {
    goto NO_ERROR;
  }

  if (uint_array[0] != 0) {
    goto NO_ERROR;
  }
  if (uint_array[1] != 2147483647) {
    goto NO_ERROR;
  }
  if (uint_array[2] != 2147483649u) {
    goto NO_ERROR;
  }
  if (uint_array[3] != 4294967295u) {
    goto NO_ERROR;
  }

  if (longlong_array[0] != -9223372036854775807l) {
    goto NO_ERROR;
  }
  if (longlong_array[1] != -2147483650l) {
    goto NO_ERROR;
  }
  if (longlong_array[2] != 2147483649l) {
    goto NO_ERROR;
  }
  if (longlong_array[3] != 9223372036854775807l) {
    goto NO_ERROR;
  }

  if (ulonglong_array[0] != 0) {
    goto NO_ERROR;
  }
  if (ulonglong_array[1] != 9223372036854775807lu) {
    goto NO_ERROR;
  }
  if (ulonglong_array[2] != 9223372036854775808lu) {
    goto NO_ERROR;
  }
  if (ulonglong_array[3] != 18446744073709551615lu) {
    goto NO_ERROR;
  }

  if (longlong_array[0] != -9223372036854775807ll) {
    goto NO_ERROR;
  }
  if (longlong_array[1] != -2147483650ll) {
    goto NO_ERROR;
  }
  if (longlong_array[2] != 2147483649ll) {
    goto NO_ERROR;
  }
  if (longlong_array[3] != 9223372036854775807ll) {
    goto NO_ERROR;
  }

  if (ulonglong_array[0] != 0) {
    goto NO_ERROR;
  }
  if (ulonglong_array[1] != 9223372036854775807ll) {
    goto NO_ERROR;
  }
  if (ulonglong_array[2] != 9223372036854775808llu) {
    goto NO_ERROR;
  }
  if (ulonglong_array[3] != 18446744073709551615llu) {
    goto NO_ERROR;
  }

  // Test under/overflows
  uchar_array[3]++; // Overflow to min of type
  uchar_array[2]++;
  uchar_array[0]--; // Underflow to max of type
  uchar_array[1]--;
  if (uchar_array[0] != 255) {
    goto NO_ERROR;
  }
  if (uchar_array[1] != 126) {
    goto NO_ERROR;
  }
  if (uchar_array[2] != 129) {
    goto NO_ERROR;
  }
  if (uchar_array[3] != 0) {
    goto NO_ERROR;
  }

  uint_array[0]--;
  if (uint_array[0] != 4294967295u) {
    goto NO_ERROR;
  }
  uint_array[0]--;
  if (uint_array[0] != 4294967294u) {
    goto NO_ERROR;
  }

  ulong_array[0]--; // Underflow to max of type
  if (ulong_array[0] != 18446744073709551615lu) {
    goto NO_ERROR;
  }

  ulong_array[3]++; // Overflow to min of type
  if (ulong_array[3] != 0) {
    goto NO_ERROR;
  }

  ulonglong_array[3]++;
  if (ulonglong_array[3] != 0) {
    goto NO_ERROR;
  }
  if (ulonglong_array[2] != 9223372036854775808llu) {  // Assert that there is no change in [2]
    goto NO_ERROR;
  }


  goto ERROR;

  NO_ERROR:
  return 0;

  ERROR:
  free(char_array); // Invalid for memsafety as well 
  return 1; // Expected
}
