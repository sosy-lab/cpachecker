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
import org.sosy_lab.cpachecker.cfa.types.Type;

import javax.annotation.Nullable;

public interface CType extends Type {

  public boolean isConst();

  @Override
  public abstract String toString();

  public boolean isVolatile();

  /**
   * Check whether the current type is *incomplete* as defined by the C standard in § 6.2.5 (1).
   * Incomplete types miss some information (e.g., <code>struct s;</code>),
   * and for example their size cannot be computed.
   */
  public boolean isIncomplete();

  /**
   * Will throw a UnsupportedOperationException
   */
  @Override
  public int hashCode();

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj);

  public abstract <R, X extends Exception> R accept(CTypeVisitor<R, X> visitor) throws X;

  public CType getCanonicalType();

  public CType getCanonicalType(boolean forceConst, boolean forceVolatile);
}
