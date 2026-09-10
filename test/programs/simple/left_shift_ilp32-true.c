// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  /*
   * C11 6.5.7 applies integer promotions before shifting.
   * The shift count must be less than the width of the promoted left operand.
   * Thus a full-width shift is not allowed; these tests use half-width and
   * width-minus-one counts where the shifted mathematical value is valid.
   * Negative signed values are never left-shifted, and no test relies on
   * signed overflow or unsigned modulo wraparound.
   */

  /* _Bool promotes to 32-bit int. */
  const _Bool bool_min = 0;
  const _Bool bool_max = 1;
  const _Bool bool_zero = 0;
  const _Bool bool_one = 1;
  const _Bool bool_negative_one = -1;
  const _Bool bool_max_minus_one = 0;
  const _Bool bool_min_minus_one = -1;
  if ((bool_min << 1) != (0)) {
    goto ERROR;
  }
  if ((bool_max << 1) != (2)) {
    goto ERROR;
  }
  if ((bool_zero << 1) != (0)) {
    goto ERROR;
  }
  if ((bool_one << 1) != (2)) {
    goto ERROR;
  }
  if ((bool_negative_one << 1) != (2)) {
    goto ERROR;
  }
  if ((bool_max_minus_one << 1) != (0)) {
    goto ERROR;
  }
  if ((bool_min_minus_one << 1) != (2)) {
    goto ERROR;
  }
  if ((bool_zero << 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((bool_one << 16) != (65536)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((bool_one << 30) != (1073741824)) {  /* largest nonzero signed result that is a power of two */
    goto ERROR;
  }
  if ((bool_zero << 31) != (0)) {  /* largest valid count; nonzero 1 << 31 would be undefined for int */
    goto ERROR;
  }

  /* Plain char is signed in the CPAchecker LINUX32/LINUX64 models. */
  const char char_max = 127;
  const char char_zero = 0;
  const char char_one = 1;
  const char char_max_minus_one = 126;
  /* char minimum and -1 are negative and therefore are not left-shifted. */
  if ((char_max << 1) != (254)) {
    goto ERROR;
  }
  if ((char_zero << 1) != (0)) {
    goto ERROR;
  }
  if ((char_one << 1) != (2)) {
    goto ERROR;
  }
  if ((char_max_minus_one << 1) != (252)) {
    goto ERROR;
  }
  if ((char_max << 16) != (8323072)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((char_zero << 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((char_one << 16) != (65536)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((char_max_minus_one << 16) != (8257536)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((char_one << 30) != (1073741824)) {  /* largest safe power-of-two result in int */
    goto ERROR;
  }
  if ((char_zero << 31) != (0)) {  /* largest valid count after promotion */
    goto ERROR;
  }
  /* char has no representable minimum-minus-one value. */

  /* signed char promotes to int. */
  const signed char signed_char_max = 127;
  const signed char signed_char_zero = 0;
  const signed char signed_char_one = 1;
  const signed char signed_char_max_minus_one = 126;
  /* signed char minimum and -1 are negative and therefore are not left-shifted. */
  if ((signed_char_max << 1) != (254)) {
    goto ERROR;
  }
  if ((signed_char_zero << 1) != (0)) {
    goto ERROR;
  }
  if ((signed_char_one << 1) != (2)) {
    goto ERROR;
  }
  if ((signed_char_max_minus_one << 1) != (252)) {
    goto ERROR;
  }
  if ((signed_char_max << 16) != (8323072)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((signed_char_zero << 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((signed_char_one << 16) != (65536)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((signed_char_max_minus_one << 16) != (8257536)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((signed_char_one << 30) != (1073741824)) {  /* largest safe power-of-two result in int */
    goto ERROR;
  }
  if ((signed_char_zero << 31) != (0)) {  /* largest valid count after promotion */
    goto ERROR;
  }
  /* signed char has no representable minimum-minus-one value. */

  /* short promotes to int. */
  const short short_max = 32767;
  const short short_zero = 0;
  const short short_one = 1;
  const short short_max_minus_one = 32766;
  /* short minimum and -1 are negative and therefore are not left-shifted. */
  if ((short_max << 1) != (65534)) {
    goto ERROR;
  }
  if ((short_zero << 1) != (0)) {
    goto ERROR;
  }
  if ((short_one << 1) != (2)) {
    goto ERROR;
  }
  if ((short_max_minus_one << 1) != (65532)) {
    goto ERROR;
  }
  if ((short_max << 16) != (2147418112)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((short_zero << 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((short_one << 16) != (65536)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((short_max_minus_one << 16) != (2147352576)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((short_one << 30) != (1073741824)) {  /* largest safe power-of-two result in int */
    goto ERROR;
  }
  if ((short_zero << 31) != (0)) {  /* largest valid count after promotion */
    goto ERROR;
  }
  /* short has no representable minimum-minus-one value. */

  /* unsigned char also promotes to int on these models. */
  const unsigned char unsigned_char_min = 0;
  const unsigned char unsigned_char_max = 255;
  const unsigned char unsigned_char_zero = 0;
  const unsigned char unsigned_char_one = 1;
  const unsigned char unsigned_char_negative_one = 255;
  const unsigned char unsigned_char_max_minus_one = 254;
  const unsigned char unsigned_char_min_minus_one = 255;
  if ((unsigned_char_min << 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_char_max << 1) != (510)) {
    goto ERROR;
  }
  if ((unsigned_char_zero << 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_char_one << 1) != (2)) {
    goto ERROR;
  }
  if ((unsigned_char_negative_one << 1) != (510)) {
    goto ERROR;
  }
  if ((unsigned_char_max_minus_one << 1) != (508)) {
    goto ERROR;
  }
  if ((unsigned_char_min_minus_one << 1) != (510)) {
    goto ERROR;
  }
  if ((unsigned_char_min << 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_max << 16) != (16711680)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_zero << 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_one << 16) != (65536)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_negative_one << 16) != (16711680)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_max_minus_one << 16) != (16646144)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_min_minus_one << 16) != (16711680)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_one << 30) != (1073741824)) {  /* largest safe power-of-two result in int */
    goto ERROR;
  }
  if ((unsigned_char_zero << 31) != (0)) {  /* largest valid count after promotion */
    goto ERROR;
  }

  /* unsigned short also promotes to int on these models. */
  const unsigned short unsigned_short_min = 0;
  const unsigned short unsigned_short_max = 65535;
  const unsigned short unsigned_short_zero = 0;
  const unsigned short unsigned_short_one = 1;
  const unsigned short unsigned_short_negative_one = 65535;
  const unsigned short unsigned_short_max_minus_one = 65534;
  const unsigned short unsigned_short_min_minus_one = 65535;
  if ((unsigned_short_min << 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_short_max << 1) != (131070)) {
    goto ERROR;
  }
  if ((unsigned_short_zero << 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_short_one << 1) != (2)) {
    goto ERROR;
  }
  if ((unsigned_short_negative_one << 1) != (131070)) {
    goto ERROR;
  }
  if ((unsigned_short_max_minus_one << 1) != (131068)) {
    goto ERROR;
  }
  if ((unsigned_short_min_minus_one << 1) != (131070)) {
    goto ERROR;
  }
  if ((unsigned_short_min << 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_zero << 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_one << 16) != (65536)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_max << 15) != (2147450880)) {  /* largest safe shift for this maximum without overflowing int */
    goto ERROR;
  }
  if ((unsigned_short_max_minus_one << 15) != (2147418112)) {
    goto ERROR;
  }
  if ((unsigned_short_one << 30) != (1073741824)) {  /* largest safe power-of-two result in int */
    goto ERROR;
  }
  if ((unsigned_short_zero << 31) != (0)) {  /* largest valid count after promotion */
    goto ERROR;
  }

  /* int: negative minimum and -1 are not left-shifted. */
  if ((2147483647 << 0) != (2147483647)) {  /* maximum is valid only with count 0 here */
    goto ERROR;
  }
  if ((0 << 1) != (0)) {
    goto ERROR;
  }
  if ((1 << 1) != (2)) {
    goto ERROR;
  }
  if ((2147483646 << 0) != (2147483646)) {
    goto ERROR;
  }
  if ((0 << 16) != (0)) {  /* half width */
    goto ERROR;
  }
  if ((1 << 16) != (65536)) {  /* half width */
    goto ERROR;
  }
  if ((1 << 30) != (1073741824)) {  /* largest safe power-of-two result */
    goto ERROR;
  }
  if ((0 << 31) != (0)) {  /* largest valid count; nonzero shift into sign bit would be undefined */
    goto ERROR;
  }
  /* int minimum minus one is not representable and is not evaluated. */

  /* unsigned int: tests avoid modulo wraparound. */
  if ((0U << 1) != (0U)) {  /* minimum/zero */
    goto ERROR;
  }
  if ((4294967295U << 0) != (4294967295U)) {  /* maximum; nonzero shift would wrap */
    goto ERROR;
  }
  if ((1U << 1) != (2U)) {
    goto ERROR;
  }
  if ((4294967295U << 0) != (4294967295U)) {  /* negative one converted to unsigned */
    goto ERROR;
  }
  if ((4294967294U << 0) != (4294967294U)) {  /* maximum minus one */
    goto ERROR;
  }
  if ((4294967295U << 0) != (4294967295U)) {  /* minimum minus one converted to unsigned */
    goto ERROR;
  }
  if ((0U << 16) != (0U)) {  /* half width */
    goto ERROR;
  }
  if ((1U << 16) != (65536U)) {  /* half width */
    goto ERROR;
  }
  if ((0U << 31) != (0U)) {  /* largest valid count */
    goto ERROR;
  }
  if ((1U << 31) != (2147483648U)) {  /* top bit, still representable as unsigned */
    goto ERROR;
  }

  /* long: negative minimum and -1 are not left-shifted. */
  if ((2147483647L << 0) != (2147483647L)) {  /* maximum is valid only with count 0 here */
    goto ERROR;
  }
  if ((0L << 1) != (0L)) {
    goto ERROR;
  }
  if ((1L << 1) != (2L)) {
    goto ERROR;
  }
  if ((2147483646L << 0) != (2147483646L)) {
    goto ERROR;
  }
  if ((0L << 16) != (0L)) {  /* half width */
    goto ERROR;
  }
  if ((1L << 16) != (65536L)) {  /* half width */
    goto ERROR;
  }
  if ((1L << 30) != (1073741824L)) {  /* largest safe power-of-two result */
    goto ERROR;
  }
  if ((0L << 31) != (0L)) {  /* largest valid count; nonzero shift into sign bit would be undefined */
    goto ERROR;
  }
  /* long minimum minus one is not representable and is not evaluated. */

  /* unsigned long: tests avoid modulo wraparound. */
  if ((0UL << 1) != (0UL)) {  /* minimum/zero */
    goto ERROR;
  }
  if ((4294967295UL << 0) != (4294967295UL)) {  /* maximum; nonzero shift would wrap */
    goto ERROR;
  }
  if ((1UL << 1) != (2UL)) {
    goto ERROR;
  }
  if ((4294967295UL << 0) != (4294967295UL)) {  /* negative one converted to unsigned */
    goto ERROR;
  }
  if ((4294967294UL << 0) != (4294967294UL)) {  /* maximum minus one */
    goto ERROR;
  }
  if ((4294967295UL << 0) != (4294967295UL)) {  /* minimum minus one converted to unsigned */
    goto ERROR;
  }
  if ((0UL << 16) != (0UL)) {  /* half width */
    goto ERROR;
  }
  if ((1UL << 16) != (65536UL)) {  /* half width */
    goto ERROR;
  }
  if ((0UL << 31) != (0UL)) {  /* largest valid count */
    goto ERROR;
  }
  if ((1UL << 31) != (2147483648UL)) {  /* top bit, still representable as unsigned */
    goto ERROR;
  }

  /* long long: negative minimum and -1 are not left-shifted. */
  if ((9223372036854775807LL << 0) != (9223372036854775807LL)) {  /* maximum is valid only with count 0 here */
    goto ERROR;
  }
  if ((0LL << 1) != (0LL)) {
    goto ERROR;
  }
  if ((1LL << 1) != (2LL)) {
    goto ERROR;
  }
  if ((9223372036854775806LL << 0) != (9223372036854775806LL)) {
    goto ERROR;
  }
  if ((0LL << 32) != (0LL)) {  /* half width */
    goto ERROR;
  }
  if ((1LL << 32) != (4294967296LL)) {  /* half width */
    goto ERROR;
  }
  if ((1LL << 62) != (4611686018427387904LL)) {  /* largest safe power-of-two result */
    goto ERROR;
  }
  if ((0LL << 63) != (0LL)) {  /* largest valid count; nonzero shift into sign bit would be undefined */
    goto ERROR;
  }
  /* long long minimum minus one is not representable and is not evaluated. */

  /* unsigned long long: tests avoid modulo wraparound. */
  if ((0ULL << 1) != (0ULL)) {  /* minimum/zero */
    goto ERROR;
  }
  if ((18446744073709551615ULL << 0) != (18446744073709551615ULL)) {  /* maximum; nonzero shift would wrap */
    goto ERROR;
  }
  if ((1ULL << 1) != (2ULL)) {
    goto ERROR;
  }
  if ((18446744073709551615ULL << 0) != (18446744073709551615ULL)) {  /* negative one converted to unsigned */
    goto ERROR;
  }
  if ((18446744073709551614ULL << 0) != (18446744073709551614ULL)) {  /* maximum minus one */
    goto ERROR;
  }
  if ((18446744073709551615ULL << 0) != (18446744073709551615ULL)) {  /* minimum minus one converted to unsigned */
    goto ERROR;
  }
  if ((0ULL << 32) != (0ULL)) {  /* half width */
    goto ERROR;
  }
  if ((1ULL << 32) != (4294967296ULL)) {  /* half width */
    goto ERROR;
  }
  if ((0ULL << 63) != (0ULL)) {  /* largest valid count */
    goto ERROR;
  }
  if ((1ULL << 63) != (9223372036854775808ULL)) {  /* top bit, still representable as unsigned */
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
