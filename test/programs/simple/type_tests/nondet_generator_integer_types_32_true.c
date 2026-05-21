// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <stdbool.h> // Enable bools for testing
#include <assert.h>
#include <stdio.h> // For size_t


// This file should be kept up to date with the SV-COMPs defined __VERIFIER_nondet_X() API!
// __VERIFIER_nondet_X() defined types according to SV-COMP 2026:
// bool, char, int, int128, float, double, loff_t, long, longlong, pchar, pthread_t, sector_t, 
// short, size_t, u32, uchar, uint, uint128, ulong, ulonglong, unsigned, ushort
// This program only tests numeric types. So pchar etc. are not tested!


extern bool __VERIFIER_nondet_bool(void);

extern char __VERIFIER_nondet_char(void);
extern unsigned char __VERIFIER_nondet_uchar(void);

extern short __VERIFIER_nondet_short(void);
extern unsigned short __VERIFIER_nondet_ushort(void);

extern int __VERIFIER_nondet_int(void);
extern unsigned int __VERIFIER_nondet_uint(void);
extern unsigned __VERIFIER_nondet_unsigned(void); // shorthand for unsigned int

// Can happen in the competition for example
typedef unsigned char u8;
typedef unsigned short u16;
typedef unsigned int u32;
typedef unsigned long long u64;
extern u8 __VERIFIER_nondet_u8(void);   // Legacy. Not explicitly listed, but sometimes used. Just a unsigned char for ILP32.
extern u16 __VERIFIER_nondet_u16(void); // Legacy. Not explicitly listed, but sometimes used. Just a unsigned short for ILP32.
extern u32 __VERIFIER_nondet_u32(void); // Legacy. Just a unsigned int for ILP32.
extern u64 __VERIFIER_nondet_u64(void); // Legacy. Not explicitly listed, but sometimes used. Just a unsigned long long for ILP32.

extern size_t __VERIFIER_nondet_size_t(void); // Defined as unsigned and large enough to hold the return of the sizeof() function. At least 65535 big.

extern long __VERIFIER_nondet_long(void);
extern unsigned long __VERIFIER_nondet_ulong(void);

extern long long __VERIFIER_nondet_longlong(void);
extern unsigned long long __VERIFIER_nondet_ulonglong(void);

extern __int128 __VERIFIER_nondet_int128(void); // GCC extension. Only available on 64 bit systems before C23!
extern unsigned __int128 __VERIFIER_nondet_uint128(void); // GCC extension. Only available on 64 bit systems before C23!

extern float __VERIFIER_nondet_float(void);
extern double __VERIFIER_nondet_double(void);


// TODO: add test program that tests casting to correct range (essentially this program, but starting from a type thats larger.)

// Safe for AssertionSafety only if all return the correct result
// This tests that an analysis sticks to the correct types of values in ILP32
// If you have doubts about whether this program has signed under/overflows:
// you can check for signed overflows in GCC using the flag -ftrapv (the program stops if an overflow is detected)
int main() {
  
  // Bools
  // Notes: bools are just defined as 0 (false) and 1 (true). 
  // Hence every calculation is just performed as if there is these numbers.
  // Casting to bool (bool == _Bool) however work the following: 0 becomes false, all non 0 become true.
  assert(false + 1 == true);
  assert((bool) (true + 1) == true);
  assert(true != false);
  assert(false + false == false);
  assert(true + false == true);
  assert((bool) (true + true) == true);
  assert(true < 2);
  assert(true < 2);
  assert(false > -1);
  assert(false > -1);
  assert((bool) 2 < 2);
  assert((bool) 2 > -1);
  assert((bool) -1 < 2);
  assert((bool) -1 > -1);

  bool nondet_bool = __VERIFIER_nondet_bool();
  assert(nondet_bool == true || nondet_bool == false); // Your analysis is unsound if this fails
  assert(nondet_bool != 2); // Your analysis is unsound if this fails
  assert(nondet_bool != 127); // Your analysis is unsound if this fails
  assert(nondet_bool != 32767); // Your analysis is unsound if this fails
  assert(nondet_bool != 65535); // Your analysis is unsound if this fails
  assert(nondet_bool != 2147483647); // Your analysis is unsound if this fails
  assert(nondet_bool < 2); // Your analysis is unsound if this fails
  assert(nondet_bool != -1); // Your analysis is unsound if this fails
  assert(nondet_bool != -2147483648); // Your analysis is unsound if this fails
  assert(nondet_bool > -1); // Your analysis is unsound if this fails

  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert((bool) (__VERIFIER_nondet_bool() + 1) == true); // Either (bool) 1 == true or (bool) 2 == true
  assert(__VERIFIER_nondet_bool() != 2); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_bool() != 127); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_bool() != 32767); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_bool() != 65535); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_bool() != 2147483647); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_bool() < 2); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_bool() != -1); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_bool() != -2147483648); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_bool() > -1); // Your analysis is unsound if this fails


  // Chars
  // Establish min and max of type char, as well as correct casting for it
  assert((char) -129 == 127);
  assert((char) 128 == -128);

  char nondet_char = __VERIFIER_nondet_char();
  assert(nondet_char != 128); // Your analysis is unsound if this fails
  assert(nondet_char != -129); // Your analysis is unsound if this fails
  assert(nondet_char < 128); // Your analysis is unsound if this fails
  assert(nondet_char > -129); // Your analysis is unsound if this fails

  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_char() != 128); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_char() != -129); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_char() < 128); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_char() > -129); // Your analysis is unsound if this fails

  // Establish the max and min of unsigned chars and their casting
  assert(((unsigned char) (255 + 1)) == 0);
  assert(((unsigned char) (0 - 1)) == 255);

  unsigned char nondet_uchar = __VERIFIER_nondet_uchar();
  assert(nondet_uchar != 256); // Your analysis is unsound if this fails
  assert(nondet_uchar != -1); // Your analysis is unsound if this fails
  assert(nondet_uchar < 256); // Your analysis is unsound if this fails
  assert(nondet_uchar > -1); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_uchar() != 256); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_uchar() != -1); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_uchar() < 256); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_uchar() > -1); // Your analysis is unsound if this fails

  u8 nondet_u8 = __VERIFIER_nondet_u8();
  assert(nondet_u8 != 256); // Your analysis is unsound if this fails
  assert(nondet_u8 != -1); // Your analysis is unsound if this fails
  assert(nondet_u8 < 256); // Your analysis is unsound if this fails
  assert(nondet_u8 > -1); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_u8() != 256); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u8() != -1); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u8() < 256); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u8() > -1); // Your analysis is unsound if this fails


  // Shorts
  // Establish min and max of type short, as well as correct casting for it
  assert((short) -32769 == 32767);
  assert((short) 32768 == -32768);

  short nondet_short = __VERIFIER_nondet_short();
  assert(nondet_short != 32768); // Your analysis is unsound if this fails
  assert(nondet_short != -32769); // Your analysis is unsound if this fails
  assert(nondet_short < 32768); // Your analysis is unsound if this fails
  assert(nondet_short > -32769); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_short() != 32768); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_short() != -32769); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_short() < 32768); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_short() > -32769); // Your analysis is unsound if this fails

  // Establish min and max of unsigned short, as well as correct casting for it
  assert(((unsigned short) (65535 + 1)) == 0);
  assert(((unsigned short) (0 - 1)) == 65535);

  unsigned short nondet_ushort = __VERIFIER_nondet_ushort();
  assert(nondet_ushort != 65536); // Your analysis is unsound if this fails
  assert(nondet_ushort != -1); // Your analysis is unsound if this fails
  assert(nondet_ushort < 65536); // Your analysis is unsound if this fails
  assert(nondet_ushort > -1); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_ushort() != 65536); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_ushort() != -1); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_ushort() < 65536); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_ushort() > -1); // Your analysis is unsound if this fails

  size_t nondet_size_t = __VERIFIER_nondet_size_t();
  assert(nondet_size_t != 65536); // Your analysis is unsound if this fails
  assert(nondet_size_t != -1); // Your analysis is unsound if this fails
  assert(nondet_size_t < 65536); // Your analysis is unsound if this fails
  assert(nondet_size_t > -1); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_size_t() != 65536); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_size_t() != -1); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_size_t() < 65536); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_size_t() > -1); // Your analysis is unsound if this fails

  u16 nondet_u16 = __VERIFIER_nondet_u16();
  assert(nondet_u16 != 65536); // Your analysis is unsound if this fails
  assert(nondet_u16 != -1); // Your analysis is unsound if this fails
  assert(nondet_u16 < 65536); // Your analysis is unsound if this fails
  assert(nondet_u16 > -1); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_u16() != 65536); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u16() != -1); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u16() < 65536); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u16() > -1); // Your analysis is unsound if this fails


  // Integers
  // Establish min and max of type int, as well as correct casting for it
  assert((int) (-2147483649ll) != 2147483648ll);
  assert((int) (2147483648ll) != -2147483649ll);

  int nondet_int = __VERIFIER_nondet_int();
  assert(nondet_int != 2147483648ll); // Your analysis is unsound if this fails
  assert(nondet_int != -2147483649ll); // Your analysis is unsound if this fails
  assert(nondet_int < 2147483648ll); // Your analysis is unsound if this fails
  assert(nondet_int > -2147483649ll); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_int() != 2147483648ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_int() != -2147483649ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_int() < 2147483648ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_int() > -2147483649ll); // Your analysis is unsound if this fails

  // Base tests for unsigned overflows to establish unsigned int min and max bounds
  assert(4294967295u + 1 == 0);
  assert(0u - 1 == 4294967295);
  assert(4294967295u + 1 == 0u);
  assert(0u - 1 == 4294967295u);
  assert(4294967295u + 1u == 0);
  assert(0 - 1u == 4294967295);
  assert(4294967295u + 1u == 0u);
  assert(0 - 1u == 4294967295u);

  unsigned int nondet_uint = __VERIFIER_nondet_uint();
  assert(nondet_uint != 4294967296ll); // Your analysis is unsound if this fails
  assert(nondet_uint != -1ll); // Your analysis is unsound if this fails
  assert(nondet_uint < 4294967296ll); // Your analysis is unsound if this fails
  assert(nondet_uint > -1ll); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_uint() != 4294967296ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_uint() != -1ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_uint() < 4294967296ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_uint() > -1ll); // Your analysis is unsound if this fails

  unsigned nondet_unsigned = __VERIFIER_nondet_unsigned();
  assert(nondet_unsigned != 4294967296ll); // Your analysis is unsound if this fails
  assert(nondet_unsigned != -1ll); // Your analysis is unsound if this fails
  assert(nondet_unsigned < 4294967296ll); // Your analysis is unsound if this fails
  assert(nondet_unsigned > -1ll); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_unsigned() != 4294967296ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_unsigned() != -1ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_unsigned() < 4294967296ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_unsigned() > -1ll); // Your analysis is unsound if this fails

  u32 nondet_u32 = __VERIFIER_nondet_u32();
  assert(nondet_u32 != 4294967296ll); // Your analysis is unsound if this fails
  assert(nondet_u32 != -1ll); // Your analysis is unsound if this fails
  assert(nondet_u32 < 4294967296ll); // Your analysis is unsound if this fails
  assert(nondet_u32 > -1ll); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_u32() != 4294967296ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u32() != -1ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u32() < 4294967296ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u32() > -1ll); // Your analysis is unsound if this fails


  // Longs
  long nondet_long = __VERIFIER_nondet_long();
  assert(nondet_long != 2147483648ll); // Your analysis is unsound if this fails
  assert(nondet_long != -2147483649ll); // Your analysis is unsound if this fails
  assert(nondet_long < 2147483648ll); // Your analysis is unsound if this fails
  assert(nondet_long > -2147483649ll); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_long() != 2147483648ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_long() != -2147483649ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_long() < 2147483648ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_long() > -2147483649ll); // Your analysis is unsound if this fails

  unsigned long nondet_ulong = __VERIFIER_nondet_ulong();
  assert(nondet_ulong != 4294967296ll); // Your analysis is unsound if this fails
  assert(nondet_ulong != -1ll); // Your analysis is unsound if this fails
  assert(nondet_ulong < 4294967296ll); // Your analysis is unsound if this fails
  assert(nondet_ulong > -1ll); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_ulong() != 4294967296ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_ulong() != -1ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_ulong() < 4294967296ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_ulong() > -1ll); // Your analysis is unsound if this fails


  // TODO: (unsigned) Long Longs need to be checked with __int128, but int128 is not available in 32bit
  /*

  // Long longs
  (in ILP32 they are larger than int and long and ranges from -9223372036854775808 to 9223372036854775807)
  long long nondet_longlong = __VERIFIER_nondet_longlong();
  assert(nondet_ != maximumPlusOne); // Your analysis is unsound if this fails
  assert(nondet_ != minimumMinOne); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_ != maximumPlusOne); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_ != minimumMinusOne); // Your analysis is unsound if this fails

  unsigned long long nondet_ulonglong = __VERIFIER_nondet_ulonglong();
  assert(nondet_ != maximumPlusOne); // Your analysis is unsound if this fails
  assert(nondet_ != minimumMinOne); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_ != maximumPlusOne); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_ != minimumMinusOne); // Your analysis is unsound if this fails


  u64 nondet_u64 = __VERIFIER_nondet_u64();
  assert(nondet_u64 != maximumPlusOne); // Your analysis is unsound if this fails
  assert(nondet_u64 != minimumMinOne); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  assert(__VERIFIER_nondet_u64() != 4294967296ll); // Your analysis is unsound if this fails
  assert(__VERIFIER_nondet_u64() != minimumMinusOne); // Your analysis is unsound if this fails
  */

  // Int128
  // TODO: can we test this accurately by using a subtraction or something like it?
  // __int128 nondet_int128 = __VERIFIER_nondet_int128();
  // assert(nondet_int128 != maximumPlusOne); // Your analysis is unsound if this fails
  // assert(nondet_int128 != minimumMinOne); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  // assert(__VERIFIER_nondet_int128() != maximumPlusOne); // Your analysis is unsound if this fails
  // assert(__VERIFIER_nondet_ != minimumMinusOne); // Your analysis is unsound if this fails

  // TODO: can we test this accurately by using a subtraction or something like it?
  // unsigned __int128 nondet_uint128 = __VERIFIER_nondet_uint128();
  // assert(nondet_uint128 != maximumPlusOne); // Your analysis is unsound if this fails
  // assert(nondet_uint128 != minimumMinOne); // Your analysis is unsound if this fails
  
  // Test that correct types are directly enforced, not only after assigning to a variable!
  // assert(__VERIFIER_nondet_ != maximumPlusOne); // Your analysis is unsound if this fails
  // assert(__VERIFIER_nondet_ != minimumMinusOne); // Your analysis is unsound if this fails

  return 0;
}
