// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

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
public interface SdgNode<P, T, V> {

  /**
   * Returns the id of this node.
   *
   * <p>Node ids are unique inside a system dependence graph.
   *
   * @return the id of this node
   */
  int getId();

  /**
   * Returns the type of the node.
   *
   * @return the type of the node
   */
  SdgNodeType getType();

  /**
   * Returns the procedure of the node.
   *
   * @return the procedure of the node
   */
  P getProcedure();

  /**
   * Returns the statement of the node.
   *
   * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
   *
   * @return the statement of the node.
   */
  Optional<T> getStatement();

  /**
   * Returns the variable of the node.
   *
   * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
   *
   * @return the variable of the node
   */
  Optional<V> getVariable();
}
