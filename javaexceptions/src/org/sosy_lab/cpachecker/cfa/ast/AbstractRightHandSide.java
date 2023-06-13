// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * Abstract Super class for all possible right-hand sides of an assignment. This class is only
 * SuperClass of all abstract Classes and their Subclasses. The Interface {@link ARightHandSide}
 * contains all language specific AST Nodes as well.
 */
public abstract class AbstractRightHandSide extends AbstractAstNode implements ARightHandSide {

  private static final long serialVersionUID = 8144915127675011353L;
  private final Type type;

  protected AbstractRightHandSide(FileLocation pFileLocation, Type pType) {
    super(pFileLocation);
    type = pType;
  }

  @Override
  public Type getExpressionType() {
    return type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(type);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AbstractRightHandSide) || !super.equals(obj)) {
      return false;
    }

    AbstractRightHandSide other = (AbstractRightHandSide) obj;

    return Objects.equals(other.type, type);
  }
}
