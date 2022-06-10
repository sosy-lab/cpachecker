// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

import java.util.Objects;
import java.util.Optional;

public abstract class AbstractSdgNode<P, T, V> implements SdgNode<P, T, V> {

  private final int id;
  private final SdgNodeType type;
  private final P procedure;
  private final Optional<T> statement;
  private final Optional<V> variable;

  private final int hash;

  AbstractSdgNode(
      int pId, SdgNodeType pType, P pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

    id = pId;
    type = pType;
    procedure = pProcedure;
    statement = pStatement;
    variable = pVariable;

    hash = Objects.hash(id, type, procedure, statement, variable);
  }

  /**
   * Creates a new {@link SdgNode} instance from the specified node.
   *
   * <p>The constructed node is a copy of the specified node. This non-private constructor is
   * required for subclasses of {@link SdgNode}.
   *
   * @param pNode a node to create a copy of
   */
  protected AbstractSdgNode(SdgNode<P, T, V> pNode) {
    this(
        pNode.getId(),
        pNode.getType(),
        pNode.getProcedure(),
        pNode.getStatement(),
        pNode.getVariable());
  }

  @Override
  public final int getId() {
    return id;
  }

  @Override
  public final SdgNodeType getType() {
    return type;
  }

  @Override
  public final P getProcedure() {
    return procedure;
  }

  @Override
  public final Optional<T> getStatement() {
    return statement;
  }

  @Override
  public final Optional<V> getVariable() {
    return variable;
  }

  @Override
  public final int hashCode() {
    return hash;
  }

  @Override
  public final boolean equals(Object pObject) {

    if (this == pObject) {
      return true;
    }

    if (!(pObject instanceof SdgNode)) {
      return false;
    }

    SdgNode<?, ?, ?> other = (SdgNode<?, ?, ?>) pObject;

    return id == other.getId()
        && type == other.getType()
        && Objects.equals(procedure, other.getProcedure())
        && Objects.equals(statement, other.getStatement())
        && Objects.equals(variable, other.getVariable());
  }

  @Override
  public final String toString() {
    return String.format(
        "%s[id=%d, type=%s, procedure=%s, statement=%s, variable=%s]",
        getClass().getName(), id, type, procedure, statement, variable);
  }
}
