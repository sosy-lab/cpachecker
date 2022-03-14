// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

/**
 * Represents an invariant witness. Use this class for interaction with the invariant store. Within
 * CPAchecker other data structures are more common to represent invariants.
 *
 * <p>This class is immutable (because ExpressionTrees and FileLocation are immutable).
 *
 * <p>Note: The class contains both: fileLocation and CFANode. There is no strict relationship
 * enforced between them, because a precise mapping is not possible (a file location corresponds to
 * several nodes and vice versa). However, both properties are required, as the node is needed in
 * most algorithms within CPAchecker and the fileLocation is needed for exporting/importing. As a
 * consequence unequal witnesses are (depending on the context) equal, e.g. witnesses with the same
 * location but different nodes look equal after exporting them.
 */
public class InvariantWitness {
  private final ExpressionTree<Object> formula;
  private final CFANode node;
  private final FileLocation location;

  InvariantWitness(ExpressionTree<Object> pFormula, FileLocation pLocation, CFANode pNode) {
    formula = Objects.requireNonNull(pFormula);
    node = Objects.requireNonNull(pNode);
    location = Objects.requireNonNull(pLocation);
  }

  public FileLocation getLocation() {
    return location;
  }

  public CFANode getNode() {
    return node;
  }

  public ExpressionTree<Object> getFormula() {
    return formula;
  }

  @Override
  public int hashCode() {
    int hashCode = location.hashCode();
    hashCode = 31 * hashCode + formula.hashCode();
    hashCode = 31 * hashCode + node.hashCode();
    return hashCode;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    }

    if (!(pObj instanceof InvariantWitness)) {
      return false;
    }
    InvariantWitness other = (InvariantWitness) pObj;

    return other.formula.equals(formula)
        && other.location.equals(location)
        && other.node.equals(node);
  }

  @Override
  public String toString() {
    return formula.toString();
  }
}
