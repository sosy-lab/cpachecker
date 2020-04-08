/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.collect.ComparisonChain;
import java.io.Serializable;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;

/**
 * The objects of the class are used to keep the set of currently tracked fields in a {@link
 * PersistentSortedMap}. Objects of {@link CompositeField} are used as keys and place-holders of
 * type {@link Boolean} are used as values.
 *
 * <p>This allows one to check if a particular field is tracked using a temporary object of {@link
 * CompositeField} and keep the set of currently tracked fields in rather simple way (no
 * special-case merging is required).
 */
@javax.annotation.concurrent.Immutable // cannot prove deep immutability
final class CompositeField implements Comparable<CompositeField>, Serializable {

  private static final long serialVersionUID = -5194535211223682619L;

  private final CCompositeType compositeType;
  private final String compositeTypeStr; // used for comparisons to allow sorting instances

  private final CCompositeTypeMemberDeclaration field;

  private CompositeField(
      final CCompositeType pCompositeType,
      final CCompositeTypeMemberDeclaration pField) {
    compositeType = pCompositeType;
    compositeTypeStr = CTypeUtils.typeToString(pCompositeType);
    field = pField;
  }

  static CompositeField of(final CCompositeType pCompositeType, final String pFieldName) {
    // search field in composite type
    for (final CCompositeTypeMemberDeclaration declaration : pCompositeType.getMembers()) {
      if (declaration.getName().equals(pFieldName)) {
        return new CompositeField(pCompositeType, declaration);
      }
    }
    throw new AssertionError(
        "Tried to start tracking for a non-existent field "
            + pFieldName
            + " in composite type "
            + pCompositeType);
  }

  static CompositeField of(
      final CCompositeType pCompositeType, final CCompositeTypeMemberDeclaration pField) {
    return new CompositeField(pCompositeType, pField);
  }

  /** Return the simplified type of the struct/union that owns this field. */
  CCompositeType getOwnerType() {
    return compositeType;
  }

  CCompositeTypeMemberDeclaration getFieldDeclaration() {
    return field;
  }

  String getFieldName() {
    return field.getName();
  }

  @Override
  public String toString() {
    return compositeTypeStr + "." + getFieldName();
  }

  @Override
  public int compareTo(final CompositeField other) {
    return ComparisonChain.start()
        .compare(this.compositeTypeStr, other.compositeTypeStr)
        .compare(this.getFieldName(), other.getFieldName())
        .result();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof CompositeField)) {
      return false;
    } else {
      CompositeField other = (CompositeField) obj;
      return compositeTypeStr.equals(other.compositeTypeStr)
          && getFieldName().equals(other.getFieldName());
    }
  }

  @Override
  public int hashCode() {
    return compositeTypeStr.hashCode() * 17 + getFieldName().hashCode();
  }
}