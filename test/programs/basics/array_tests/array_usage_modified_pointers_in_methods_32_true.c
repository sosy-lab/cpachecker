// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>
#include <stdlib.h>


void assertModPointerToArraysUsageWithSubscriptAndPointer(char (*char_a)[3], long (*long_a)[3], unsigned long long (*ulonglong_a)[5]) {

  assert(*((*char_a) - 3) == 25);
  assert(((*char_a) - 3)[2] == 75);

  assert((*char_a)[0] == 100);
  assert((*(char_a - 1))[2] == 75);
  assert(*((*(char_a - 1)) + 3) == 100);


  assert((*long_a)[0] == 79);
  assert((*long_a)[1] == 104);

  assert(((*long_a) - 2)[0] == 29);
  assert(((*long_a) - 2)[1] == 54);

  assert((*ulonglong_a)[1] == 34);
  assert(*((*ulonglong_a) + 2) == 59);
  assert(((*ulonglong_a) + 2)[1] == 84);
  assert((*ulonglong_a)[4] == 109);
}


void assertPointerToArraysUsageWithSubscriptAndPointer(int *int_a_from_long, char char_a[], unsigned long long *ulonglong_a) {

  assert(char_a[0] == 25);
  assert(*(char_a + 1) == 50);
  assert(char_a[2] == 75);
  assert(char_a[3] == 100);

  assert(int_a_from_long[0] == 29);
  assert(*(int_a_from_long + 1) == 54);
  assert(int_a_from_long[2] == 79);
  assert(int_a_from_long[3] == 104);

  assert(ulonglong_a[0] == 34);
  assert(*(ulonglong_a + 1) == 59);
  assert(ulonglong_a[2] == 84);
  assert(ulonglong_a[3] == 109);

  // Increment pointers (char increments by the size of the entire array (the 3 sized!!!!) due to casting happening before increment!!!!!)
  assertModPointerToArraysUsageWithSubscriptAndPointer((char (*)[3]) char_a + 1, (int (*)[3]) (int_a_from_long + 2), (unsigned long long (*)[5]) (ulonglong_a - 1));
}


// There will be some warning about the casts of long to int to long pointers/array pointers, this is intentional!
void assertPointerToArraysUsageWithSubscript(unsigned long long (*ulonglong_a)[4], int (*long_a_as_int)[4], char (*char_a)[4]) {

  assert((*char_a)[0] == 25);
  assert((*char_a)[1] == 50);
  assert((*char_a)[2] == 75);
  assert((*char_a)[3] == 100);

  assert((*long_a_as_int)[0] == 29);
  assert((*long_a_as_int)[1] == 54);
  assert((*long_a_as_int)[2] == 79);
  assert((*long_a_as_int)[3] == 104);

  assert((*ulonglong_a)[0] == 34);
  assert((*ulonglong_a)[1] == 59);
  assert((*ulonglong_a)[2] == 84);
  assert((*ulonglong_a)[3] == 109);

  assertPointerToArraysUsageWithSubscriptAndPointer(*long_a_as_int, *char_a, *ulonglong_a);
}


// Unsigned long and int behave the same for long in ILP32 (ulong only for positive values that are not too large)
int * assignArraysNested(unsigned long * ulong_ptr_to_array) {
  ulong_ptr_to_array[1] = 0; // We override this after returning (this is the last element in the array)
  *ulong_ptr_to_array = 32768; // Third element in the array
  return ((int *) ulong_ptr_to_array) + 1;
}


// We mix subscript and pointer derefs here and assign new values
unsigned long * assignArrays(unsigned long long (*ulonglong_ptr_array)[4], long (*long_array)[4], char (*char_array)[4], char * char_ptr) {
  // Assign new values (w min and max of the types) and check again
  ((char *) (*char_array))[0] = -128;
  (*char_array)[1] = -1;
  (char_ptr)[2] = 1;
  (char_ptr)[3] = 127;


  (*long_array)[3] = -11; // We overwrite this in the nested assignment 
  // int and long are of the same size in ILP32
  int (*int_array_ptr_from_nested)[1] = (int (*)[1]) assignArraysNested((*long_array) + 2);
  (*int_array_ptr_from_nested)[0] = 2147483647; // Last element in the array

  *(*long_array) = -2147483648;
  long_array++; // Now we incremented the size of the entire array and (*long_array)[0] is out of bounds!
  int_array_ptr_from_nested = (int (*)[1]) long_array;
  (*(int_array_ptr_from_nested - 3))[0] = -32769; // Assign to second entry in the array 


  *(*ulonglong_ptr_array) = 18446744073709551610llu;
  unsigned long long * ulonglong_ptr = *ulonglong_ptr_array;
  ulonglong_ptr++; // This does not effect the previous variables holding pointers etc. of this array
  (ulonglong_ptr - 2)[2] = 9223372036854775807llu;
  *(ulonglong_ptr + 1) = 13;
  ((*ulonglong_ptr_array) + 1)[1] = 9223372036854775808llu;
  ulonglong_ptr[2] = 18446744073709551615llu;

  return (*long_array) - 2; // long_array was "+4 (long objects)" to the original pointer before this line and is "+2" in the returned address
}

// This returns the pointer towards the first long array element - 1 long object as a char*
char * assertArraysAfterAssignmentAndChangeSome3(char * char_array_1, long long_1[], unsigned long long (*ulonglong_array_2)[3]) {

  // Assert some and change some again
  assert(*(char_array_1 - 1) == -128); // Input pointers are already incremented!
  char_array_1--; // Reverse the increment
  assert(char_array_1[1] == -1);
  assert(*(char_array_1 + 2) == 1);
  assert(*(char_array_1 + 3) == 127);
  (char_array_1 + 2)[1]--; // Dec the 4th element (127 before)

  assert(*((*ulonglong_array_2) - 2) == 18446744073709551610llu);
  assert(*(*ulonglong_array_2) == 9223372036854775808llu);
  assert((*ulonglong_array_2)[1] == 18446744073709551615llu);
  unsigned long long (*ulonglong_array_ptr_min_1)[4] = (unsigned long long (*)[4]) ((*ulonglong_array_2) - 3);

  assert((*ulonglong_array_ptr_min_1)[2] == 9223372036854775807ll);
  assert(*((*ulonglong_array_ptr_min_1) + 3) == 9223372036854775808llu);
  assert((*ulonglong_array_ptr_min_1)[4] == 18446744073709551615llu);

  assert((*(ulonglong_array_2 - 1))[1] == 18446744073709551610llu); // initial array element
  assert(((*ulonglong_array_2) - 3)[1] == 18446744073709551610llu); // initial array element

  // Dec the value, and not the ptr
  (*ulonglong_array_ptr_min_1)[1]--; // Results in binary 1111111111111111111111111111111111111111111111111111111111111001, i.e. hex 0xFFFFFFFFFFFFFFF9 (18446744073709551610llu - 1)

  { // New scope in function scope to test that the arrays are not dropped and changes persist
    unsigned long long tmp = (*ulonglong_array_2)[1]; // Write into original array[1] the max value for ulonglong currently in [3]
    *((*(--ulonglong_array_2)) + 2) = tmp; // move ulonglong_array_2 by 3 objects into the negative and write the value (we can't do this in 1 line due to unspecified behavior!)
    (1 + ((unsigned long long *)(*ulonglong_array_2)))[3] = 1; // original array [3] = 1;
  }
  
  ulonglong_array_2++; // Moves by pointer 3 unsigned long long elements (back to where we started in this function)
  (*ulonglong_array_2)[-1]++; // Overflows the second array value ([1]) to 0, should not affect any other value!

  (*((*ulonglong_array_2) + 1))++; // Increment the value in [3] (to 2)

  assert(*(((long *) long_1) - 1) == -2147483648);
  return ((long *) long_1 - 2);
}

void assertArraysAfterAssignment4(char (*char_a)[3], long (*long_1)[3], unsigned long long ulonglong_arrayey_neg_1[]) {
  // Assert the rest (including changes done in assertArraysAfterAssignmentAndChangeSome3())
  // And the pointer is unaffected by the increment of the array

  assert((*char_a)[0] == -128);
  assert((*char_a)[1] == -1);
  assert((*char_a)[2] == 1);
  assert(*((*char_a) + 3) == 126);

  assert(((*long_1) - 1)[0] == -2147483648);
  assert(*((*long_1) - 1) == -2147483648);
  assert(**long_1 == -32769);
  assert((*long_1)[1] == 32768);
  assert(*(*(long_1) + 2) == 2147483647);

  assert(*(((unsigned long long *)ulonglong_arrayey_neg_1) - 1) == 0xFFFFFFFFFFFFFFF9);
  unsigned long long * ulonglong_ar_ptr = ((unsigned long long *) ulonglong_arrayey_neg_1) - 1;
  assert(*ulonglong_ar_ptr == 0xFFFFFFFFFFFFFFF9);
  assert(ulonglong_ar_ptr[1] == 0);
  assert(*(ulonglong_ar_ptr + 2) == 9223372036854775808llu);
  assert(ulonglong_ar_ptr[3] == 2);
}

void assertArraysAfterAssignment2(unsigned long long * ulonglong_a, short * short_ptr_from_long_array_plus_2, char * char_a) {
  char_a++;
  // Shorts are 2 bytes, longs 4 in ILP32, so we decrement twice to get from the starting pointer that was incremented twice to only once
  short_ptr_from_long_array_plus_2--;
  ulonglong_a = ulonglong_a + 2;
  short_ptr_from_long_array_plus_2--;
  char (*char_a_ptr)[3] = (char (*)[3]) char_a;
  // assertArraysAfterAssignmentAndChangeSome3() also takes pointers, but we increment them before so that everything is +1 in assertArraysAfterAssignmentAndChangeSome3()
  int * int_0 = (int *) (assertArraysAfterAssignmentAndChangeSome3(*char_a_ptr, (long (*)[3]) short_ptr_from_long_array_plus_2, ulonglong_a) + 4);
  
  // Decrement the pointers again so that they point the the array start again
  char_a--;
  int_0++;
  ulonglong_a--;

  // assertArraysAfterAssignment4() takes a mixture of arrays and pointers
  assertArraysAfterAssignment4(char_a, int_0, ulonglong_a);
}

// This takes arrays, assertArraysAfterAssignment2 takes pointers, this should not be a problems
void assertArraysAfterAssignment(char char_array[], int int_array[], unsigned long long ulonglong_array[]) {
  assert(int_array[0] == 32768);
  assert(int_array[1] == 2147483647);
  assertArraysAfterAssignment2(ulonglong_array, int_array, char_array);
}


// Safe for AssertionSafety only if all return the correct result
// This tests that an analysis can handle arrays as pointers and array pointers in methods (local and on the stack) in ILP32 (this fails for LP64).
// This includes several switches in between the pointer types and casts to arrays and back, 
// different increments/decrements that rely on the pointer/array type used, different access types (subscript, pointer), 
// as well as type re-interpretations (but only in types that are equally large, e.g. long to int)
// Note: the warnings about incompatible pointer types when compiling are on purpose and this works as expected!
int main() {

  // Longs are 4 bytes in ILP32 (early fail for wrong machine model)
  assert(sizeof(long) == 4);
  
  char char_array[] = {25, 50, 75, 100};

  long long_array[] = {29, 54, 79, 104};

  long (*long_array_ptr_as_long_array)[4] = &long_array;

  unsigned long long ulonglong_array[] = {34, 59, 84, 109};

  // Test pointer access first. This includes incrementing the pointer, which should have no effect on the arrays here, meaning assertPointerToArraysUsageWithSubscript() checks the same values! 
  assertPointerToArraysUsageWithSubscript(&ulonglong_array, long_array_ptr_as_long_array, &char_array);

  char (*char_array_as_array_ptr)[4] = &char_array;

  // The warning in the compiler is intentional! (I want to test no explicit casting!)
  int * int_ptr = *long_array_ptr_as_long_array;

  int_ptr = assignArrays((unsigned long long (*)[4]) ulonglong_array, (long (*)[4]) int_ptr, char_array_as_array_ptr, *char_array_as_array_ptr);

  void * char_array_as_void_ptr = (void *) char_array;

  // int_ptr is the address of element long_array[2] here
  assertArraysAfterAssignment(char_array_as_void_ptr, int_ptr, ulonglong_array);

  return 0;
}
