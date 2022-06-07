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

/**
 * Represents a single node in a system dependence graph (SDG).
 *
 * <p>This class can be extended to get an easier to use type without type parameters.
 *
 * @param <P> The type of procedures in the SDG. Typically, programs are organized into procedures,
 *     functions, or similar constructs that consist of statements, expressions, and declarations.
 *     In an SDG, all these compound constructs are refered to as procedures and are of the type
 *     specified by this type parameter.
 * @param <T> The statement type of the SDG. Typically, programs consist of statements, expressions,
 *     and declarations. In an SDG, all these parts are refered to as statements and are of the type
 *     specified by this type parameter.
 * @param <V> The type of variables in the SDG. Variables are defined and used. Dependencies exist
 *     between defs and subsequent uses. Furthermore, formal-in/out and actual-in/out nodes exist
 *     for specific variables.
 */
public class SdgNode<P, T, V> {

  private final int id;
  private final SdgNodeType type;
  private final P procedure;
  private final Optional<T> statement;
  private final Optional<V> variable;

  private final int hash;

  SdgNode(int pId, SdgNodeType pType, P pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

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
  protected SdgNode(SdgNode<P, T, V> pNode) {
    this(pNode.id, pNode.type, pNode.procedure, pNode.statement, pNode.variable);
  }

  /**
   * Returns the id of this node.
   *
   * <p>Node ids are unique inside a system dependence graph.
   *
   * @return the id of this node
   */
  public final int getId() {
    return id;
  }

  /**
   * Returns the type of the node.
   *
   * @return the type of the node
   */
  public final SdgNodeType getType() {
    return type;
  }

  /**
   * Returns the procedure of the node.
   *
   * @return the procedure of the node
   */
  public final P getProcedure() {
    return procedure;
  }

  /**
   * Returns the statement of the node.
   *
   * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
   *
   * @return the statement of the node.
   */
  public final Optional<T> getStatement() {
    return statement;
  }

  /**
   * Returns the variable of the node.
   *
   * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
   *
   * @return the variable of the node
   */
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

    return id == other.id
        && hash == other.hash
        && type == other.type
        && Objects.equals(procedure, other.procedure)
        && Objects.equals(statement, other.statement)
        && Objects.equals(variable, other.variable);
  }

  @Override
  public final String toString() {
    return String.format(
        "%s[id=%d, type=%s, procedure=%s, statement=%s, variable=%s]",
        getClass().getName(), id, type, procedure, statement, variable);
  }
}
