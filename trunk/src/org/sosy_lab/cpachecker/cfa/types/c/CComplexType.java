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
package org.sosy_lab.cpachecker.cfa.types.c;


/**
 * Interface for types representing enums, structs, and unions.
 */
public interface CComplexType extends CType {

  ComplexTypeKind getKind();

  /**
   * Returns the unqualified name, e.g. for the type "struct s", this returns "s".
   * @return A name string or the empty string if the type has no name.
   */
  String getName();

  /**
   * Returns the unqualified name, e.g. for the type "struct s", this returns "struct s".
   * If the name is empty, this contains only the qualifier.
   */
  String getQualifiedName();

  /**
   * Returns the unqualified original name, e.g. for the type "struct s", this
   * returns "."
   * @returnA name string or the empty string if the type has no name.
   */
  String getOrigName();

  /**
   * Returns true if the compared objects are equal regarding the common
   * rules for the equals method. The only difference is, that if a CComplexType
   * is anonymous (thus, the origName is an empty string) the name comparison
   * is left out.
   */
  boolean equalsWithOrigName(Object obj);

  public static enum ComplexTypeKind {
    ENUM,
    STRUCT,
    UNION;

    public String toASTString() {
      return name().toLowerCase();
    }
  }
}
