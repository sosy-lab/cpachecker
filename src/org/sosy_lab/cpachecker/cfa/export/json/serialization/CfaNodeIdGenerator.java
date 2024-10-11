// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.CFANodeMixin;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * A custom generator for generating unique IDs for {@link CFANode}s.
 *
 * <p>It uses the node number and the class name of the node to generate the ID.
 *
 * @see CfaJsonExport
 * @see CFANodeMixin
 */
public class CfaNodeIdGenerator extends AbstractStringIdGenerator {
  private static final long serialVersionUID = 1296028592781872635L;

  /* Jackson requires a default constructor for serialization. */
  public CfaNodeIdGenerator() {
    this(Object.class);
  }

  /**
   * Constructs a new {@link CfaNodeIdGenerator}.
   *
   * @param pScope The class scope for which the ID generator is created.
   */
  protected CfaNodeIdGenerator(Class<?> pScope) {
    super(pScope);
  }

  /**
   * Creates a new instance of {@link CfaNodeIdGenerator} with the specified scope.
   *
   * @param pScope The class scope for the new instance.
   * @return a new instance of {@link CfaNodeIdGenerator}.
   */
  @Override
  protected AbstractStringIdGenerator newInstance(Class<?> pScope) {
    return new CfaNodeIdGenerator(pScope);
  }

  /**
   * Generates a unique identifier for a given object.
   *
   * @param pForObject The object for which to generate an ID; must be an instance of {@link
   *     CFANode}.
   * @return a unique identifier string in the format "Node_<nodeNumber>_(<className>)".
   * @throws NullPointerException if pForObject is null.
   * @throws IllegalArgumentException if pForObject is not an instance of {@link CFANode}.
   */
  @Override
  public String generateId(Object pForObject) {
    checkNotNull(pForObject, "The object to generate an ID for must not be null");

    if (!(pForObject instanceof CFANode)) {
      throw new IllegalArgumentException(
          "The object to generate an ID for must be an instance of CFANode");
    }

    return "Node_"
        + ((CFANode) pForObject).getNodeNumber()
        + "_("
        + pForObject.getClass().getSimpleName().replace("Node", "")
        + ")";
  }
}
