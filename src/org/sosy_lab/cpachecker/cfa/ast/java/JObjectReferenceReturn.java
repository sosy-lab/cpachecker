// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import com.google.common.base.Optional;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;

/**
 * This class makes the return of an object reference to the caller of
 * an constructor explicit. Semantically, it is the equivalent of return this;
 * It may however only occur at the end of an constructor in the cfa.
 *
 * The returnClassType only provides the compile time type, i. e. the class,
 * which declared the constructor. This may not always be the case,
 * i.e. super constructor invocation.
 *
 *
 */
public final class JObjectReferenceReturn extends JReturnStatement {

  private static final long serialVersionUID = 8482771117891447280L;
  private final JClassType classReference;

  public JObjectReferenceReturn(FileLocation pFileLocation, JClassType pClassReference) {
    super(pFileLocation, Optional.of(new JThisExpression(pFileLocation, pClassReference)));
    classReference = pClassReference;
  }

  public JClassType getReturnClassType() {
    return classReference;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(classReference);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JObjectReferenceReturn)
        || !super.equals(obj)) {
      return false;
    }

    JObjectReferenceReturn other = (JObjectReferenceReturn) obj;

    return Objects.equals(other.classReference, classReference);
  }

}