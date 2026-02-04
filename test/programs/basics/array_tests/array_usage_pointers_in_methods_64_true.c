// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>
#include <stdlib.h>


void assertPointerToArraysUsageWithSubscript(unsigned long long * ulonglong_a, long long * longlong_a, unsigned long * ulong_a, long * long_a, unsigned int * uint_a, int * int_a, unsigned short * ushort_a, short * short_a, unsigned char * uchar_a, char * char_a) {

  assert(char_a[0] == 25);
  assert(char_a[1] == 50);
  assert(char_a[2] == 75);
  assert(char_a[3] == 100);

  assert(uchar_a[0] == 26);
  assert(uchar_a[1] == 51);
  assert(uchar_a[2] == 76);
  assert(uchar_a[3] == 101);

  assert(short_a[0] == 27);
  assert(short_a[1] == 52);
  assert(short_a[2] == 77);
  assert(short_a[3] == 102);

  assert(ushort_a[0] == 28);
  assert(ushort_a[1] == 53);
  assert(ushort_a[2] == 78);
  assert(ushort_a[3] == 103);

  assert(int_a[0] == 29);
  assert(int_a[1] == 54);
  assert(int_a[2] == 79);
  assert(int_a[3] == 104);

  assert(uint_a[0] == 30);
  assert(uint_a[1] == 55);
  assert(uint_a[2] == 80);
  assert(uint_a[3] == 105);

  assert(long_a[0] == 31);
  assert(long_a[1] == 56);
  assert(long_a[2] == 81);
  assert(long_a[3] == 106);

  assert(ulong_a[0] == 32);
  assert(ulong_a[1] == 57);
  assert(ulong_a[2] == 82);
  assert(ulong_a[3] == 107);

  assert(longlong_a[0] == 33);
  assert(longlong_a[1] == 58);
  assert(longlong_a[2] == 83);
  assert(longlong_a[3] == 108);

  assert(ulonglong_a[0] == 34);
  assert(ulonglong_a[1] == 59);
  assert(ulonglong_a[2] == 84);
  assert(ulonglong_a[3] == 109);
}

void assertPointerToArraysUsageAsPointer(unsigned long long * ulonglong_a, long long * longlong_a, unsigned long * ulong_a, long * long_a, unsigned int * uint_a, int * int_a, unsigned short * ushort_a, short * short_a, unsigned char * uchar_a, char * char_a) {

  assert(*char_a == 25);
  char_a++; // The address of the function parameter changes, but not the address/array of the calling function!
  assert(*char_a == 50);
  assert(*(char_a + 1) == 75); // Due to increment of char_a we only need +1 here
  assert(*(char_a + 2) == 100);

  assert(*uchar_a == 26);
  uchar_a++;
  assert(*uchar_a == 51);
  assert(*(uchar_a + 1) == 76);
  assert(*(uchar_a + 2) == 101);


  assert(*short_a == 27);
  short_a++;
  assert(*short_a == 52);
  assert(*(short_a + 1) == 77);
  assert(*(short_a + 2) == 102);

  assert(*ushort_a == 28);
  ushort_a++;
  assert(*ushort_a == 53);
  assert(*(ushort_a + 1) == 78);
  assert(*(ushort_a + 2) == 103);


  assert(*int_a == 29);
  int_a++;
  assert(*int_a == 54);
  assert(*(int_a + 1) == 79);
  assert(*(int_a + 2) == 104);

  assert(*uint_a == 30);
  uint_a++;
  assert(*uint_a == 55);
  assert(*(uint_a + 1) == 80);
  assert(*(uint_a + 2) == 105);

  assert(*long_a == 31);
  long_a++;
  assert(*long_a == 56);
  assert(*(long_a + 1) == 81);
  assert(*(long_a + 2) == 106);

  assert(*ulong_a == 32);
  ulong_a++;
  assert(*ulong_a == 57);
  assert(*(ulong_a + 1) == 82);
  assert(*(ulong_a + 2) == 107);

  assert(*longlong_a == 33);
  longlong_a++;
  assert(*longlong_a == 58);
  assert(*(longlong_a + 1) == 83);
  assert(*(longlong_a + 2) == 108);

  assert(*ulonglong_a == 34);
  ulonglong_a++;
  assert(*ulonglong_a == 59);
  assert(*(ulonglong_a + 1) == 84);
  assert(*(ulonglong_a + 2) == 109);
}

// We mix subscript and pointer derefs here
void assignArrays(unsigned long long * ulonglong_array, long long * longlong_array, unsigned long * ulong_array, long * long_array, unsigned int * uint_array, int * int_array, unsigned short * ushort_array, short * short_array, unsigned char * uchar_array, char * char_array) {
  // Assign new values (w min and max of the types) and check again
  *char_array = -128;
  char_array++;
  *char_array = -1;
  *(char_array + 1) = 1;
  char_array[2] = 127;

  *uchar_array = 0;
  uchar_array++;
  *uchar_array = 127;
  *(uchar_array + 1) = 128;
  uchar_array[2] = 255;

  *short_array = -32768;
  short_array++;
  *short_array = -129;
  *(short_array + 1) = 128;
  short_array[2] = 32767;

  *ushort_array = 0;
  ushort_array++;
  *ushort_array = 32767;
  *(ushort_array + 1) = 32768;
  ushort_array[2] = 65535;

  *int_array = -2147483648;
  int_array++;
  *int_array = -32769;
  *(int_array + 1) = 32768;
  int_array[2] = 2147483647;

  *uint_array = 0;
  uint_array++;
  *uint_array = 2147483647u;
  *(uint_array + 1) = 2147483649u;
  uint_array[2] = 4294967295u;

  *long_array = -2147483648l;
  long_array++;
  *long_array = -32769l;
  *(long_array + 1) = 32768l;
  long_array[2] = 2147483647l;

  *ulong_array = 0lu;
  ulong_array++;
  *ulong_array = 2147483647lu;
  *(ulong_array + 1) = 2147483649lu;
  ulong_array[2] = 4294967295lu;

  *longlong_array = -9223372036854775807ll;
  longlong_array++;
  *longlong_array = -2147483650ll;
  *(longlong_array + 1) = 2147483649ll;
  longlong_array[2] = 9223372036854775807ll;

  *ulonglong_array = 18446744073709551610llu;
  ulonglong_array++;
  *ulonglong_array = 9223372036854775807llu;
  *(ulonglong_array + 1) = 9223372036854775808llu;
  ulonglong_array[2] = 18446744073709551615llu;
}

// Some are not used on purpose!
void assertArraysAfterAssignmentAndChangeSome3(char * char_array, unsigned char * uchar_array, short * short_array, unsigned short * ushort_array, int * int_array, unsigned long * not_a_int, unsigned long long * ulonglong_array) {
  // Assert some and change some again
  assert(*(char_array - 1) == -128); // Input pointers are already incremented!
  char_array--; // Reverse the increment
  assert(char_array[1] == -1);
  assert(*(char_array + 2) == 1);
  assert(*(char_array + 3) == 127);

  assert(*(short_array - 1) == -32768);
  short_array--;
  assert(short_array[1] == -129);
  assert(*(short_array + 2) == 128);
  assert(*(short_array + 3) == 32767);

  // unsigned long
  assert(*(not_a_int - 1) == 0);
  not_a_int--;
  assert(not_a_int[1] == 2147483647);
  assert(*(not_a_int + 2) == 2147483649u);
  assert(*(not_a_int + 3) == 4294967295u);

  assert(*(ulonglong_array - 1) == 18446744073709551610llu);
  ulonglong_array--;
  assert(ulonglong_array[1] == 9223372036854775807ll);
  assert(*(ulonglong_array + 2) == 9223372036854775808llu);
  assert(*(ulonglong_array + 3) == 18446744073709551615llu);

  assert(*(ushort_array - 1) == 0);
  ushort_array--;
  assert(ushort_array[1] == 32767);
  assert(*(ushort_array + 2) == 32768);
  assert(*(ushort_array + 3) == 65535);

  
  // Postfix increment increments and returns the original pointer which is then dereferenced
  assert(*ushort_array++ == 0);
  // Prefix increment increments and then dereferences the incremented pointer
  assert(*++ushort_array == 32768);
  // Same applies to decrement of course
  assert(*--ushort_array == 32767);
  assert(*ushort_array-- == 32767);
  

  // Inc/Dec etc. and then check for changes in the other nested method
  not_a_int[0]--; // Underflow to max unsigned long
  *(not_a_int + 1) =  not_a_int[1] + not_a_int[2] + 1; // 2147483647 + 2147483649 + 1 == 4294967297, i.e. overflow and result is 1
  not_a_int++;
  --*not_a_int; // 1 - 1 == 0
  *(not_a_int + 1) = 2147483650u;
  not_a_int[2]++; // Overflow to 0


  ushort_array[3] = ushort_array[3] + 1; // Overflow
  ushort_array[0] = ushort_array[0] - 1; // Underflow
  { // New scope to test persistence of changes
    unsigned short tmp = *++ushort_array; // This is element at index 1 (not 0)!
    ushort_array[0] = *(ushort_array + 1); // Swap [1] and [2] ([0] and [1] due to increment)
    ushort_array[1] = tmp;
  }

  assert(*ulonglong_array == 18446744073709551610llu);
  // Dec the value, and not the ptr
  (*ulonglong_array)--; // Results in binary 1111111111111111111111111111111111111111111111111111111111111001, i.e. hex 0xFFFFFFFFFFFFFFF9
  *(++ulonglong_array) = 0; // Should not affect [0]!
  *(--ulonglong_array + 2) = 18446744073709551615llu;
  ulonglong_array++;
  ulonglong_array[2]++; // Overflow to 0, should not affect any other value!
  (*(ulonglong_array + 1))++; // Overflow to 0, should not affect any other value!
}

void assertArraysAfterAssignment4(char char_a[], unsigned char uchar_a[], short * short_a, unsigned short ushort_a[], int * int_a, unsigned int uint_a[], long long_a[], unsigned long * ulong_a, unsigned long long ulonglong_arrayey[], long long * longlong_a) {
  // Assert the rest (including changes done in assertArraysAfterAssignmentAndChangeSome3())
  unsigned char * uchar_a_ptr = uchar_a;
  assert(uchar_a_ptr[0] == 0);
  uchar_a_ptr++;
  assert(*uchar_a_ptr == 127);
  assert(uchar_a_ptr[1] == 128);
  assert(*(uchar_a_ptr + 2) == 255);

  // The initial "array" uchar_a is unaffected by the increment
  assert(uchar_a[0] == 0);
  uchar_a++;
  assert(*uchar_a == 127);
  assert(uchar_a[1] == 128);
  assert(*(uchar_a + 2) == 255);

  // And the pointer is unaffected by the increment of the array (as it was assigned before it)
  assert(uchar_a_ptr[0] == 127);
  assert(*(uchar_a_ptr + 1) == 128);
  assert(uchar_a_ptr[2] == 255);


  assert(*ushort_a == 65535);
  ushort_a++;
  assert(ushort_a[0] == 32768);
  assert(*(ushort_a + 1) == 32767);
  assert(ushort_a[2] == 0);

  assert(int_a[0] == -2147483648);
  int_a++;
  int * int_a_ptr = int_a; // Get the arrays pointer in its increment state with implicit addressof operator
  assert(*int_a_ptr == -32769);
  assert(int_a_ptr[1] == 32768);
  assert(*(int_a_ptr + 2) == 2147483647);

  assert(*uint_a == 0);
  uint_a++;
  assert(uint_a[0] == 2147483647);
  assert(*(uint_a + 1) == 2147483649u);
  assert(uint_a[2] == 4294967295u);

  assert(long_a[0] == -2147483648l);
  long_a++;
  assert(*long_a == -32769);
  assert(long_a[1] == 32768);
  assert(*(long_a + 2) == 2147483647l);

  assert(*ulong_a == 18446744073709551615lu); // These fail for the wrong type (bit) sizes!
  ulong_a++;
  assert(ulong_a[0] == 4294967296ll); // long long so that smaller bit types (ILP32) fail
  assert(*(ulong_a + 1) == 2147483650ll);
  assert(ulong_a[2] == 4294967296ll);

  assert(longlong_a[0] == -9223372036854775807ll);
  longlong_a++;
  assert(*longlong_a == -2147483650ll);
  assert(longlong_a[1] == 2147483649);
  assert(*(longlong_a + 2) == 9223372036854775807ll);

  assert(*ulonglong_arrayey == 0xFFFFFFFFFFFFFFF9);
  ulonglong_arrayey++;
  assert(ulonglong_arrayey[0] == 0);
  assert(*(ulonglong_arrayey + 1) == 0);
  assert(ulonglong_arrayey[2] == 0);
}

void assertArraysAfterAssignment2(unsigned long long * ulonglong_a, long long * longlong_a, unsigned long * ulong_a, long * long_a, unsigned int * uint_a, int * int_a, unsigned short * ushort_a, short * short_a, unsigned char * uchar_a, char * char_a) {

  char_a++;
  uchar_a++;
  short_a++;
  ushort_a++;
  int_a++;
  ulong_a++;
  ulonglong_a++;
  // assertArraysAfterAssignmentAndChangeSome3() also takes pointers, but we increment them before so that everything is +1 in assertArraysAfterAssignmentAndChangeSome3()
  assertArraysAfterAssignmentAndChangeSome3(char_a, uchar_a, short_a, ushort_a, int_a, ulong_a, ulonglong_a);
  
  // Decrement the pointers again so that they point the the array start again
  char_a--;
  uchar_a--;
  short_a--;
  ushort_a--;
  int_a--;
  ulong_a--;
  ulonglong_a--;

  // assertArraysAfterAssignment4() takes a mixture of arrays and pointers
  assertArraysAfterAssignment4(char_a, uchar_a, short_a, ushort_a, int_a, uint_a, long_a, ulong_a, ulonglong_a, longlong_a);
}

// This takes arrays, assertArraysAfterAssignment2 takes pointers, this should not be a problems
void assertArraysAfterAssignment(char char_array[], unsigned char uchar_array[], short short_array[], unsigned short ushort_array[], int int_array[], unsigned int uint_array[], long long_array[], unsigned long ulong_array[], long long longlong_array[], unsigned long long ulonglong_array[]) {
  assertArraysAfterAssignment2(ulonglong_array, longlong_array, ulong_array, long_array, uint_array, int_array, ushort_array, short_array, uchar_array, char_array);
}


// Safe for AssertionSafety only if all return the correct result
// This tests that an analysis can handle arrays as pointers in methods (local and on the stack) in LP64 (this fails for ILP32)
int main() {

  // Longs are 8 bytes in LP64 (early fail for wrong machine model)
  assert(sizeof(long) == 8);
  
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

  // Test pointer access first. This includes incrementing the pointer, which should have no effect on the arrays here, meaning assertPointerToArraysUsageWithSubscript() checks the same values! 
  assertPointerToArraysUsageAsPointer(ulonglong_array, longlong_array, ulong_array, long_array, uint_array, int_array, ushort_array, short_array, uchar_array, char_array);
  assertPointerToArraysUsageWithSubscript(ulonglong_array, longlong_array, ulong_array, long_array, uint_array, int_array, ushort_array, short_array, uchar_array, char_array);

  assignArrays(ulonglong_array, longlong_array, ulong_array, long_array, uint_array, int_array, ushort_array, short_array, uchar_array, char_array);

  assertArraysAfterAssignment(char_array, uchar_array, short_array, ushort_array, int_array, uint_array, long_array, ulong_array, longlong_array, ulonglong_array);

  return 0;
}
