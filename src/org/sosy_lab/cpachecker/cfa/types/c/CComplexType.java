// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

/** Interface for types representing enums, structs, and unions. */
public interface CComplexType extends CType {

  ComplexTypeKind getKind();

  /**
   * Returns the unqualified name, e.g. for the type "struct s", this returns "s".
   *
   * @return A name string or the empty string if the type has no name.
   */
  String getName();

  /**
   * Returns the unqualified name, e.g. for the type "struct s", this returns "struct s". If the
   * name is empty, this contains only the qualifier.
   */
  String getQualifiedName();

  /**
   * Returns the unqualified original name, e.g. for the type "struct s", this returns "."
   *
   * @return A name string or the empty string if the type has no name.
   */
  String getOrigName();

  /**
   * Returns true if the compared objects are equal regarding the common rules for the equals
   * method. The only difference is, that if a CComplexType is anonymous (thus, the origName is an
   * empty string) the name comparison is left out.
   */
  boolean equalsWithOrigName(Object obj);

  enum ComplexTypeKind {
    ENUM,
    STRUCT,
    UNION;

    public String toASTString() {
      return name().toLowerCase();
    }
  }
}
