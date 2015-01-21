/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.constraints;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicIdentifier;

/**
 * Converter for {@link SymbolicIdentifier} objects.
 * Converts SymbolicIdentifiers to and from Strings.
 */
public class IdentifierConverter {

  private static final IdentifierConverter SINGLETON = new IdentifierConverter();

  private static final String PREFIX = "s";
  private static final Type DUMMY_TYPE = new Type() {

    @Override
    public String toASTString(String declarator) {
      return "unspecified";
    }
  };

  private IdentifierConverter() {
    // DO NOTHING
  }

  public static IdentifierConverter getInstance() {
    return SINGLETON;
  }

  /**
   * Converts a given {@link SymbolicIdentifier} to a String.
   * The returned <code>String</code> contains all information necessary for uniquely identifying the given
   * identifier.
   *
   *  <p>For a given identifier p, <code>convert(convert(p)) = p</code> is always true.
   *
   * @param pIdentifier the <code>SymbolicIdentifier</code> to convert to a string
   * @return a <code>String</code> containing all information necessary for converting it to a identifier
   */
  public String convert(SymbolicIdentifier pIdentifier) {
    return PREFIX + pIdentifier.getId();
  }

  /**
   * Converts a given encoding of a {@link SymbolicIdentifier} to the corresponding
   * <code>SymbolicIdentifier</code>.
   *
   * Only valid encodings, as produced by {@link #convert(SymbolicIdentifier)}, are allowed.
   *
   * @param pIdentifierInformation a <code>String</code> encoding of a <code>SymbolicIdentifier</code>
   * @return the <code>SymbolicIdentifier</code> representing the given encoding
   */
  public SymbolicIdentifier convert(String pIdentifierInformation) {
    final long id = Long.parseLong(pIdentifierInformation.substring(PREFIX.length()));
    return new SymbolicIdentifier(id, DUMMY_TYPE);
  }

  /**
   * Returns whether the given string is a valid encoding of a {@link SymbolicIdentifier}.
   *
   * @param pName the string to analyse
   * @return <code>true</code> if the given string is a valid encoding of a <code>SymbolicIdentifier</code>,
   *    <code>false</code> otherwise
   */
  public boolean isSymbolicEncoding(String pName) {
    return pName.matches(PREFIX + "[0-9]+");
  }
}
