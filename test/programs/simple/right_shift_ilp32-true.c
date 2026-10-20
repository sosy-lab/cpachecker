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
   * width-minus-one counts instead. Right shift of a negative signed value
   * is implementation-defined; the expected values below use arithmetic
   * right shift, as used by GCC, Clang, and CPAchecker Linux machine models.
   */

  /* _Bool values promote to int, so valid counts range from 0 through 31. */
  const _Bool bool_min = 0;
  const _Bool bool_max = 1;
  const _Bool bool_zero = 0;
  const _Bool bool_one = 1;
  const _Bool bool_negative_one = -1;
  const _Bool bool_max_minus_one = 0;
  const _Bool bool_min_minus_one = -1;
  if ((bool_min >> 1) != (0)) {
    goto ERROR;
  }
  if ((bool_max >> 1) != (0)) {
    goto ERROR;
  }
  if ((bool_zero >> 1) != (0)) {
    goto ERROR;
  }
  if ((bool_one >> 1) != (0)) {
    goto ERROR;
  }
  if ((bool_negative_one >> 1) != (0)) {
    goto ERROR;
  }
  if ((bool_max_minus_one >> 1) != (0)) {
    goto ERROR;
  }
  if ((bool_min_minus_one >> 1) != (0)) {
    goto ERROR;
  }
  if ((bool_max >> 16) != (0)) {  /* half of the promoted 32-bit width */
    goto ERROR;
  }
  if ((bool_one >> 31) != (0)) {  /* largest valid count for the promoted int */
    goto ERROR;
  }
  if ((bool_negative_one >> 31) != (0)) {  /* -1 converted to _Bool is 1 */
    goto ERROR;
  }

  /* Plain char is signed in the CPAchecker LINUX32/LINUX64 models. */
  const char char_min = -128;
  const char char_max = 127;
  const char char_zero = 0;
  const char char_one = 1;
  const char char_negative_one = -1;
  const char char_max_minus_one = 126;
  if ((char_min >> 1) != (-64)) {  /* arithmetic right shift */
    goto ERROR;
  }
  if ((char_max >> 1) != (63)) {
    goto ERROR;
  }
  if ((char_zero >> 1) != (0)) {
    goto ERROR;
  }
  if ((char_one >> 1) != (0)) {
    goto ERROR;
  }
  if ((char_negative_one >> 1) != (-1)) {  /* arithmetic right shift */
    goto ERROR;
  }
  if ((char_max_minus_one >> 1) != (63)) {
    goto ERROR;
  }
  if ((char_min >> 16) != (-1)) {  /* half of promoted width; arithmetic right shift */
    goto ERROR;
  }
  if ((char_max >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((char_zero >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((char_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((char_negative_one >> 16) != (-1)) {  /* half of promoted width; arithmetic right shift */
    goto ERROR;
  }
  if ((char_max_minus_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((char_min >> 31) != (-1)) {  /* largest valid count after integer promotion; arithmetic right shift */
    goto ERROR;
  }
  if ((char_max >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((char_zero >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((char_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((char_negative_one >> 31) != (-1)) {  /* largest valid count after integer promotion; arithmetic right shift */
    goto ERROR;
  }
  if ((char_max_minus_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  /* char has no representable minimum-minus-one value. */

  /* signed char promotes to int on the tested models. */
  const signed char signed_char_min = -128;
  const signed char signed_char_max = 127;
  const signed char signed_char_zero = 0;
  const signed char signed_char_one = 1;
  const signed char signed_char_negative_one = -1;
  const signed char signed_char_max_minus_one = 126;
  if ((signed_char_min >> 1) != (-64)) {  /* arithmetic right shift */
    goto ERROR;
  }
  if ((signed_char_max >> 1) != (63)) {
    goto ERROR;
  }
  if ((signed_char_zero >> 1) != (0)) {
    goto ERROR;
  }
  if ((signed_char_one >> 1) != (0)) {
    goto ERROR;
  }
  if ((signed_char_negative_one >> 1) != (-1)) {  /* arithmetic right shift */
    goto ERROR;
  }
  if ((signed_char_max_minus_one >> 1) != (63)) {
    goto ERROR;
  }
  if ((signed_char_min >> 16) != (-1)) {  /* half of promoted width; arithmetic right shift */
    goto ERROR;
  }
  if ((signed_char_max >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((signed_char_zero >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((signed_char_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((signed_char_negative_one >> 16) != (-1)) {  /* half of promoted width; arithmetic right shift */
    goto ERROR;
  }
  if ((signed_char_max_minus_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((signed_char_min >> 31) != (-1)) {  /* largest valid count after integer promotion; arithmetic right shift */
    goto ERROR;
  }
  if ((signed_char_max >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((signed_char_zero >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((signed_char_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((signed_char_negative_one >> 31) != (-1)) {  /* largest valid count after integer promotion; arithmetic right shift */
    goto ERROR;
  }
  if ((signed_char_max_minus_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  /* signed char has no representable minimum-minus-one value. */

  /* unsigned char promotes to int on the tested models. */
  const unsigned char unsigned_char_min = 0;
  const unsigned char unsigned_char_max = 255;
  const unsigned char unsigned_char_zero = 0;
  const unsigned char unsigned_char_one = 1;
  const unsigned char unsigned_char_negative_one = (unsigned char)-1;
  const unsigned char unsigned_char_max_minus_one = 254;
  const unsigned char unsigned_char_min_minus_one = (unsigned char)-1;
  if ((unsigned_char_min >> 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_char_max >> 1) != (127)) {
    goto ERROR;
  }
  if ((unsigned_char_zero >> 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_char_one >> 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_char_negative_one >> 1) != (127)) {
    goto ERROR;
  }
  if ((unsigned_char_max_minus_one >> 1) != (127)) {
    goto ERROR;
  }
  if ((unsigned_char_min_minus_one >> 1) != (127)) {
    goto ERROR;
  }
  if ((unsigned_char_min >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_max >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_zero >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_negative_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_max_minus_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_min_minus_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_char_min >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_char_max >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_char_zero >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_char_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_char_negative_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_char_max_minus_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_char_min_minus_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }

  /* short promotes to int on the tested models. */
  const short short_min = -32768;
  const short short_max = 32767;
  const short short_zero = 0;
  const short short_one = 1;
  const short short_negative_one = -1;
  const short short_max_minus_one = 32766;
  if ((short_min >> 1) != (-16384)) {  /* arithmetic right shift */
    goto ERROR;
  }
  if ((short_max >> 1) != (16383)) {
    goto ERROR;
  }
  if ((short_zero >> 1) != (0)) {
    goto ERROR;
  }
  if ((short_one >> 1) != (0)) {
    goto ERROR;
  }
  if ((short_negative_one >> 1) != (-1)) {  /* arithmetic right shift */
    goto ERROR;
  }
  if ((short_max_minus_one >> 1) != (16383)) {
    goto ERROR;
  }
  if ((short_min >> 16) != (-1)) {  /* half of promoted width; arithmetic right shift */
    goto ERROR;
  }
  if ((short_max >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((short_zero >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((short_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((short_negative_one >> 16) != (-1)) {  /* half of promoted width; arithmetic right shift */
    goto ERROR;
  }
  if ((short_max_minus_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((short_min >> 31) != (-1)) {  /* largest valid count after integer promotion; arithmetic right shift */
    goto ERROR;
  }
  if ((short_max >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((short_zero >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((short_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((short_negative_one >> 31) != (-1)) {  /* largest valid count after integer promotion; arithmetic right shift */
    goto ERROR;
  }
  if ((short_max_minus_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  /* short has no representable minimum-minus-one value. */

  /* unsigned short promotes to int on the tested models. */
  const unsigned short unsigned_short_min = 0;
  const unsigned short unsigned_short_max = 65535;
  const unsigned short unsigned_short_zero = 0;
  const unsigned short unsigned_short_one = 1;
  const unsigned short unsigned_short_negative_one = (unsigned short)-1;
  const unsigned short unsigned_short_max_minus_one = 65534;
  const unsigned short unsigned_short_min_minus_one = (unsigned short)-1;
  if ((unsigned_short_min >> 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_short_max >> 1) != (32767)) {
    goto ERROR;
  }
  if ((unsigned_short_zero >> 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_short_one >> 1) != (0)) {
    goto ERROR;
  }
  if ((unsigned_short_negative_one >> 1) != (32767)) {
    goto ERROR;
  }
  if ((unsigned_short_max_minus_one >> 1) != (32767)) {
    goto ERROR;
  }
  if ((unsigned_short_min_minus_one >> 1) != (32767)) {
    goto ERROR;
  }
  if ((unsigned_short_min >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_max >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_zero >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_negative_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_max_minus_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_min_minus_one >> 16) != (0)) {  /* half of promoted width */
    goto ERROR;
  }
  if ((unsigned_short_min >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_short_max >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_short_zero >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_short_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_short_negative_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_short_max_minus_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }
  if ((unsigned_short_min_minus_one >> 31) != (0)) {  /* largest valid count after integer promotion */
    goto ERROR;
  }

  /* int: direct literals have the required type. */
  if (((-2147483647 - 1) >> 1) != (-1073741824)) {  /* minimum, shift by 1; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if (((-2147483647 - 1) >> 16) != (-32768)) {  /* minimum, shift by 16; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if (((-2147483647 - 1) >> 31) != (-1)) {  /* minimum, shift by 31; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((2147483647 >> 1) != (1073741823)) {  /* maximum, shift by 1 */
    goto ERROR;
  }
  if ((2147483647 >> 16) != (32767)) {  /* maximum, shift by 16 */
    goto ERROR;
  }
  if ((2147483647 >> 31) != (0)) {  /* maximum, shift by 31 */
    goto ERROR;
  }
  if ((0 >> 1) != (0)) {  /* zero, shift by 1 */
    goto ERROR;
  }
  if ((0 >> 16) != (0)) {  /* zero, shift by 16 */
    goto ERROR;
  }
  if ((0 >> 31) != (0)) {  /* zero, shift by 31 */
    goto ERROR;
  }
  if ((1 >> 1) != (0)) {  /* one, shift by 1 */
    goto ERROR;
  }
  if ((1 >> 16) != (0)) {  /* one, shift by 16 */
    goto ERROR;
  }
  if ((1 >> 31) != (0)) {  /* one, shift by 31 */
    goto ERROR;
  }
  if ((-1 >> 1) != (-1)) {  /* negative one, shift by 1; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((-1 >> 16) != (-1)) {  /* negative one, shift by 16; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((-1 >> 31) != (-1)) {  /* negative one, shift by 31; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((2147483646 >> 1) != (1073741823)) {  /* maximum minus one, shift by 1 */
    goto ERROR;
  }
  if ((2147483646 >> 16) != (32767)) {  /* maximum minus one, shift by 16 */
    goto ERROR;
  }
  if ((2147483646 >> 31) != (0)) {  /* maximum minus one, shift by 31 */
    goto ERROR;
  }
  /* int minimum minus one is not representable and is not evaluated. */

  /* unsigned int: direct unsigned literals, no arithmetic wraparound is used. */
  if ((0U >> 1) != (0U)) {  /* minimum, shift by 1 */
    goto ERROR;
  }
  if ((0U >> 16) != (0U)) {  /* minimum, shift by 16 */
    goto ERROR;
  }
  if ((0U >> 31) != (0U)) {  /* minimum, shift by 31 */
    goto ERROR;
  }
  if ((4294967295U >> 1) != (2147483647U)) {  /* maximum, shift by 1 */
    goto ERROR;
  }
  if ((4294967295U >> 16) != (65535U)) {  /* maximum, shift by 16 */
    goto ERROR;
  }
  if ((4294967295U >> 31) != (1U)) {  /* maximum, shift by 31 */
    goto ERROR;
  }
  if ((0U >> 1) != (0U)) {  /* zero, shift by 1 */
    goto ERROR;
  }
  if ((0U >> 16) != (0U)) {  /* zero, shift by 16 */
    goto ERROR;
  }
  if ((0U >> 31) != (0U)) {  /* zero, shift by 31 */
    goto ERROR;
  }
  if ((1U >> 1) != (0U)) {  /* one, shift by 1 */
    goto ERROR;
  }
  if ((1U >> 16) != (0U)) {  /* one, shift by 16 */
    goto ERROR;
  }
  if ((1U >> 31) != (0U)) {  /* one, shift by 31 */
    goto ERROR;
  }
  if ((4294967295U >> 1) != (2147483647U)) {  /* negative one converted to unsigned, shift by 1 */
    goto ERROR;
  }
  if ((4294967295U >> 16) != (65535U)) {  /* negative one converted to unsigned, shift by 16 */
    goto ERROR;
  }
  if ((4294967295U >> 31) != (1U)) {  /* negative one converted to unsigned, shift by 31 */
    goto ERROR;
  }
  if ((4294967294U >> 1) != (2147483647U)) {  /* maximum minus one, shift by 1 */
    goto ERROR;
  }
  if ((4294967294U >> 16) != (65535U)) {  /* maximum minus one, shift by 16 */
    goto ERROR;
  }
  if ((4294967294U >> 31) != (1U)) {  /* maximum minus one, shift by 31 */
    goto ERROR;
  }
  if ((4294967295U >> 1) != (2147483647U)) {  /* minimum minus one converted to unsigned, shift by 1 */
    goto ERROR;
  }
  if ((4294967295U >> 16) != (65535U)) {  /* minimum minus one converted to unsigned, shift by 16 */
    goto ERROR;
  }
  if ((4294967295U >> 31) != (1U)) {  /* minimum minus one converted to unsigned, shift by 31 */
    goto ERROR;
  }

  /* long: direct literals have the required type. */
  if (((-2147483647L - 1L) >> 1) != (-1073741824L)) {  /* minimum, shift by 1; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if (((-2147483647L - 1L) >> 16) != (-32768L)) {  /* minimum, shift by 16; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if (((-2147483647L - 1L) >> 31) != (-1L)) {  /* minimum, shift by 31; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((2147483647L >> 1) != (1073741823L)) {  /* maximum, shift by 1 */
    goto ERROR;
  }
  if ((2147483647L >> 16) != (32767L)) {  /* maximum, shift by 16 */
    goto ERROR;
  }
  if ((2147483647L >> 31) != (0L)) {  /* maximum, shift by 31 */
    goto ERROR;
  }
  if ((0L >> 1) != (0L)) {  /* zero, shift by 1 */
    goto ERROR;
  }
  if ((0L >> 16) != (0L)) {  /* zero, shift by 16 */
    goto ERROR;
  }
  if ((0L >> 31) != (0L)) {  /* zero, shift by 31 */
    goto ERROR;
  }
  if ((1L >> 1) != (0L)) {  /* one, shift by 1 */
    goto ERROR;
  }
  if ((1L >> 16) != (0L)) {  /* one, shift by 16 */
    goto ERROR;
  }
  if ((1L >> 31) != (0L)) {  /* one, shift by 31 */
    goto ERROR;
  }
  if ((-1L >> 1) != (-1L)) {  /* negative one, shift by 1; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((-1L >> 16) != (-1L)) {  /* negative one, shift by 16; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((-1L >> 31) != (-1L)) {  /* negative one, shift by 31; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((2147483646L >> 1) != (1073741823L)) {  /* maximum minus one, shift by 1 */
    goto ERROR;
  }
  if ((2147483646L >> 16) != (32767L)) {  /* maximum minus one, shift by 16 */
    goto ERROR;
  }
  if ((2147483646L >> 31) != (0L)) {  /* maximum minus one, shift by 31 */
    goto ERROR;
  }
  /* long minimum minus one is not representable and is not evaluated. */

  /* unsigned long: direct unsigned literals, no arithmetic wraparound is used. */
  if ((0UL >> 1) != (0UL)) {  /* minimum, shift by 1 */
    goto ERROR;
  }
  if ((0UL >> 16) != (0UL)) {  /* minimum, shift by 16 */
    goto ERROR;
  }
  if ((0UL >> 31) != (0UL)) {  /* minimum, shift by 31 */
    goto ERROR;
  }
  if ((4294967295UL >> 1) != (2147483647UL)) {  /* maximum, shift by 1 */
    goto ERROR;
  }
  if ((4294967295UL >> 16) != (65535UL)) {  /* maximum, shift by 16 */
    goto ERROR;
  }
  if ((4294967295UL >> 31) != (1UL)) {  /* maximum, shift by 31 */
    goto ERROR;
  }
  if ((0UL >> 1) != (0UL)) {  /* zero, shift by 1 */
    goto ERROR;
  }
  if ((0UL >> 16) != (0UL)) {  /* zero, shift by 16 */
    goto ERROR;
  }
  if ((0UL >> 31) != (0UL)) {  /* zero, shift by 31 */
    goto ERROR;
  }
  if ((1UL >> 1) != (0UL)) {  /* one, shift by 1 */
    goto ERROR;
  }
  if ((1UL >> 16) != (0UL)) {  /* one, shift by 16 */
    goto ERROR;
  }
  if ((1UL >> 31) != (0UL)) {  /* one, shift by 31 */
    goto ERROR;
  }
  if ((4294967295UL >> 1) != (2147483647UL)) {  /* negative one converted to unsigned, shift by 1 */
    goto ERROR;
  }
  if ((4294967295UL >> 16) != (65535UL)) {  /* negative one converted to unsigned, shift by 16 */
    goto ERROR;
  }
  if ((4294967295UL >> 31) != (1UL)) {  /* negative one converted to unsigned, shift by 31 */
    goto ERROR;
  }
  if ((4294967294UL >> 1) != (2147483647UL)) {  /* maximum minus one, shift by 1 */
    goto ERROR;
  }
  if ((4294967294UL >> 16) != (65535UL)) {  /* maximum minus one, shift by 16 */
    goto ERROR;
  }
  if ((4294967294UL >> 31) != (1UL)) {  /* maximum minus one, shift by 31 */
    goto ERROR;
  }
  if ((4294967295UL >> 1) != (2147483647UL)) {  /* minimum minus one converted to unsigned, shift by 1 */
    goto ERROR;
  }
  if ((4294967295UL >> 16) != (65535UL)) {  /* minimum minus one converted to unsigned, shift by 16 */
    goto ERROR;
  }
  if ((4294967295UL >> 31) != (1UL)) {  /* minimum minus one converted to unsigned, shift by 31 */
    goto ERROR;
  }

  /* long long: direct literals have the required type. */
  if (((-9223372036854775807LL - 1LL) >> 1) != (-4611686018427387904LL)) {  /* minimum, shift by 1; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if (((-9223372036854775807LL - 1LL) >> 32) != (-2147483648LL)) {  /* minimum, shift by 32; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if (((-9223372036854775807LL - 1LL) >> 63) != (-1LL)) {  /* minimum, shift by 63; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((9223372036854775807LL >> 1) != (4611686018427387903LL)) {  /* maximum, shift by 1 */
    goto ERROR;
  }
  if ((9223372036854775807LL >> 32) != (2147483647LL)) {  /* maximum, shift by 32 */
    goto ERROR;
  }
  if ((9223372036854775807LL >> 63) != (0LL)) {  /* maximum, shift by 63 */
    goto ERROR;
  }
  if ((0LL >> 1) != (0LL)) {  /* zero, shift by 1 */
    goto ERROR;
  }
  if ((0LL >> 32) != (0LL)) {  /* zero, shift by 32 */
    goto ERROR;
  }
  if ((0LL >> 63) != (0LL)) {  /* zero, shift by 63 */
    goto ERROR;
  }
  if ((1LL >> 1) != (0LL)) {  /* one, shift by 1 */
    goto ERROR;
  }
  if ((1LL >> 32) != (0LL)) {  /* one, shift by 32 */
    goto ERROR;
  }
  if ((1LL >> 63) != (0LL)) {  /* one, shift by 63 */
    goto ERROR;
  }
  if ((-1LL >> 1) != (-1LL)) {  /* negative one, shift by 1; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((-1LL >> 32) != (-1LL)) {  /* negative one, shift by 32; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((-1LL >> 63) != (-1LL)) {  /* negative one, shift by 63; implementation-defined arithmetic right shift */
    goto ERROR;
  }
  if ((9223372036854775806LL >> 1) != (4611686018427387903LL)) {  /* maximum minus one, shift by 1 */
    goto ERROR;
  }
  if ((9223372036854775806LL >> 32) != (2147483647LL)) {  /* maximum minus one, shift by 32 */
    goto ERROR;
  }
  if ((9223372036854775806LL >> 63) != (0LL)) {  /* maximum minus one, shift by 63 */
    goto ERROR;
  }
  /* long long minimum minus one is not representable and is not evaluated. */

  /* unsigned long long: direct unsigned literals, no arithmetic wraparound is used. */
  if ((0ULL >> 1) != (0ULL)) {  /* minimum, shift by 1 */
    goto ERROR;
  }
  if ((0ULL >> 32) != (0ULL)) {  /* minimum, shift by 32 */
    goto ERROR;
  }
  if ((0ULL >> 63) != (0ULL)) {  /* minimum, shift by 63 */
    goto ERROR;
  }
  if ((18446744073709551615ULL >> 1) != (9223372036854775807ULL)) {  /* maximum, shift by 1 */
    goto ERROR;
  }
  if ((18446744073709551615ULL >> 32) != (4294967295ULL)) {  /* maximum, shift by 32 */
    goto ERROR;
  }
  if ((18446744073709551615ULL >> 63) != (1ULL)) {  /* maximum, shift by 63 */
    goto ERROR;
  }
  if ((0ULL >> 1) != (0ULL)) {  /* zero, shift by 1 */
    goto ERROR;
  }
  if ((0ULL >> 32) != (0ULL)) {  /* zero, shift by 32 */
    goto ERROR;
  }
  if ((0ULL >> 63) != (0ULL)) {  /* zero, shift by 63 */
    goto ERROR;
  }
  if ((1ULL >> 1) != (0ULL)) {  /* one, shift by 1 */
    goto ERROR;
  }
  if ((1ULL >> 32) != (0ULL)) {  /* one, shift by 32 */
    goto ERROR;
  }
  if ((1ULL >> 63) != (0ULL)) {  /* one, shift by 63 */
    goto ERROR;
  }
  if ((18446744073709551615ULL >> 1) != (9223372036854775807ULL)) {  /* negative one converted to unsigned, shift by 1 */
    goto ERROR;
  }
  if ((18446744073709551615ULL >> 32) != (4294967295ULL)) {  /* negative one converted to unsigned, shift by 32 */
    goto ERROR;
  }
  if ((18446744073709551615ULL >> 63) != (1ULL)) {  /* negative one converted to unsigned, shift by 63 */
    goto ERROR;
  }
  if ((18446744073709551614ULL >> 1) != (9223372036854775807ULL)) {  /* maximum minus one, shift by 1 */
    goto ERROR;
  }
  if ((18446744073709551614ULL >> 32) != (4294967295ULL)) {  /* maximum minus one, shift by 32 */
    goto ERROR;
  }
  if ((18446744073709551614ULL >> 63) != (1ULL)) {  /* maximum minus one, shift by 63 */
    goto ERROR;
  }
  if ((18446744073709551615ULL >> 1) != (9223372036854775807ULL)) {  /* minimum minus one converted to unsigned, shift by 1 */
    goto ERROR;
  }
  if ((18446744073709551615ULL >> 32) != (4294967295ULL)) {  /* minimum minus one converted to unsigned, shift by 32 */
    goto ERROR;
  }
  if ((18446744073709551615ULL >> 63) != (1ULL)) {  /* minimum minus one converted to unsigned, shift by 63 */
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
