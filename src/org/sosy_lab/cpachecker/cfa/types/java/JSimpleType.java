/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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

  private final JBasicType type;
  private final boolean isPrimitive;

  /**
   * Creates a new <code>JSimpleType</code> object that represents the given
   * basic type.
   *
   * @param pType the concrete primitive type to represent
   */
  public JSimpleType(JBasicType pType) {
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
      result = prime * result + Objects.hashCode(isPrimitive);
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

  // TODO toString erstellen
}