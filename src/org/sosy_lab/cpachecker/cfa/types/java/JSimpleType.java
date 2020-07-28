// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * Description of a simple Java structure's type.
 *
 * These descriptions are mostly merely primitive types, but include special cases like
 * <code>null</code> either. Actually, possible concrete types are all enum constants of
 * {@link JBasicType}.
 */
public class JSimpleType implements JType {

  private static final long serialVersionUID = 7153757299840260748L;

  private final JBasicType type;
  private final boolean isPrimitive;

  private final static JSimpleType SINGLETON_BOOL = new JSimpleType(JBasicType.BOOLEAN);
  private final static JSimpleType SINGLETON_BYTE = new JSimpleType(JBasicType.BYTE);
  private final static JSimpleType SINGLETON_SHORT = new JSimpleType(JBasicType.SHORT);
  private final static JSimpleType SINGLETON_CHAR = new JSimpleType(JBasicType.CHAR);
  private final static JSimpleType SINGLETON_INT = new JSimpleType(JBasicType.INT);
  private final static JSimpleType SINGLETON_LONG = new JSimpleType(JBasicType.LONG);
  private final static JSimpleType SINGLETON_FLOAT = new JSimpleType(JBasicType.FLOAT);
  private final static JSimpleType SINGLETON_DOUBLE = new JSimpleType(JBasicType.DOUBLE);
  private final static JSimpleType SINGLETON_NULL = new JSimpleType(JBasicType.NULL);
  private final static JSimpleType SINGLETON_UNSPECIFIED = new JSimpleType(JBasicType.UNSPECIFIED);
  private final static JSimpleType SINGLETON_VOID = new JSimpleType(JBasicType.VOID);

  public static JSimpleType getBoolean() {
    return SINGLETON_BOOL;
  }

  public static JSimpleType getByte() {
    return SINGLETON_BYTE;
  }

  public static JSimpleType getShort() {
    return SINGLETON_SHORT;
  }

  public static JSimpleType getChar() {
    return SINGLETON_CHAR;
  }

  public static JSimpleType getInt() {
    return SINGLETON_INT;
  }

  public static JSimpleType getLong() {
    return SINGLETON_LONG;
  }

  public static JSimpleType getFloat() {
    return SINGLETON_FLOAT;
  }

  public static JSimpleType getDouble() {
    return SINGLETON_DOUBLE;
  }

  public static JSimpleType getNull() {
    return SINGLETON_NULL;
  }

  public static JSimpleType getUnspecified() {
    return SINGLETON_UNSPECIFIED;
  }

  public static JSimpleType getVoid() {
    return SINGLETON_VOID;
  }

  /**
   * Creates a new <code>JSimpleType</code> object that represents the given
   * basic type.
   *
   * @param pType the concrete primitive type to represent
   */
  private JSimpleType(JBasicType pType) {
    type = pType;

    switch (type) {
    case BOOLEAN:
      //$FALL-THROUGH$
    case BYTE:
      //$FALL-THROUGH$
    case INT:
      //$FALL-THROUGH$
    case SHORT:
      //$FALL-THROUGH$
    case FLOAT:
      //$FALL-THROUGH$
    case DOUBLE:
      isPrimitive = true;
      break;
    default:
      isPrimitive = false;
    }

  }

  /**
   * Returns the concrete primitive type this class represents.
   *
   * @return the concrete primitive type this class represents
   */
  public JBasicType getType() {
    return type;
  }

  @Override
  public String toASTString(String pDeclarator) {
    List<String> parts = new ArrayList<>();

    parts.add(Strings.emptyToNull(type.toASTString()));
    parts.add(Strings.emptyToNull(pDeclarator));

    return Joiner.on(' ').skipNulls().join(parts);
  }

  public boolean isPrimitive() {
    return isPrimitive;
  }

  @Override
  public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(type);
      result = prime * result + Boolean.hashCode(isPrimitive);
      return result;
  }

  /**
   * Returns whether the given object equals this object.
   *
   * <p>Two <code>JSimpleType</code> objects equal each other if their stored primitive types equal.</p>
   *
   * @param obj the object to compare to this object
   * @return <code>true</code> if the given object equals this object, <code>false</code> otherwise
   */
  @Override
  public boolean equals(Object obj) {
      if (this == obj) {
          return true;
      }

      if (!(obj instanceof JSimpleType)) {
          return false;
      }

      JSimpleType other = (JSimpleType) obj;
      return type == other.type && isPrimitive == other.isPrimitive;
  }

  @Override
  public String toString() {
    switch (type) {
    case UNSPECIFIED:
      return "unspecified";
    default:
      return type.toASTString();
    }
  }
}
