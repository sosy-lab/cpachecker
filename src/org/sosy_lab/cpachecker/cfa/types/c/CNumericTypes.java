// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

/**
 * Utility class providing constant instances for all standard C numeric simple types.
 *
 * <p>These constants are pre-configured with the correct qualifiers (e.g., signed, unsigned, long,
 * short) for common C primitive types like {@code int}, {@code char}, and {@code float} .
 *
 * <p>Note: This class explicitly deals only with scalar CSimpleTypes and does not include derived
 * types such as arrays, pointers, or structures.
 */
public final class CNumericTypes {

  private CNumericTypes() {}

  // type constants
  //      const volatile  basic type    long   short  signed unsign compl  imag   long long
  public static final CSimpleType BOOL =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.BOOL, false, false, false, false, false, false, false);
  public static final CSimpleType CHAR =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.CHAR, false, false, false, false, false, false, false);
  public static final CSimpleType CONST_CHAR =
      new CSimpleType(
          CTypeQualifiers.CONST, CBasicType.CHAR, false, false, false, false, false, false, false);
  public static final CSimpleType SIGNED_CHAR =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.CHAR, false, false, true, false, false, false, false);
  public static final CSimpleType UNSIGNED_CHAR =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.CHAR, false, false, false, true, false, false, false);
  public static final CSimpleType INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, false, false, false, false, false);
  public static final CSimpleType CONST_INT =
      new CSimpleType(
          CTypeQualifiers.CONST, CBasicType.INT, false, false, false, false, false, false, false);
  public static final CSimpleType SIGNED_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, false);
  public static final CSimpleType UNSIGNED_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, false, true, false, false, false);
  public static final CSimpleType SHORT_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, true, false, false, false, false, false);
  public static final CSimpleType UNSIGNED_SHORT_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, true, false, true, false, false, false);
  public static final CSimpleType LONG_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, true, false, false, false, false, false, false);
  public static final CSimpleType SIGNED_LONG_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, true, false, true, false, false, false, false);
  public static final CSimpleType UNSIGNED_LONG_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, true, false, false, true, false, false, false);
  public static final CSimpleType LONG_LONG_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, false, false, false, false, true);
  public static final CSimpleType SIGNED_LONG_LONG_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, true, false, false, false, true);
  public static final CSimpleType UNSIGNED_LONG_LONG_INT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, false, false, false, true, false, false, true);

  public static final CSimpleType FLOAT =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.FLOAT, false, false, false, false, false, false, false);
  public static final CSimpleType DOUBLE =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.DOUBLE, false, false, false, false, false, false, false);
  public static final CSimpleType LONG_DOUBLE =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.DOUBLE, true, false, false, false, false, false, false);

  public static final CType SIZE_T =
      new CSimpleType(
          CTypeQualifiers.NONE, CBasicType.INT, true, false, false, true, false, false, false);
}
