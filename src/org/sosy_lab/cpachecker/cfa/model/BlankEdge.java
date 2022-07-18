// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdgeVisitor;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;

public class BlankEdge extends AbstractCFAEdge implements CCfaEdge {

  private static final long serialVersionUID = 6394933292868202442L;

  private final String description;

  private final Optional<StrategiesEnum> strategy;

  public BlankEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      String pDescription) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    description = pDescription;
    strategy = Optional.empty();
  }

  public BlankEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      String pDescription,
      StrategiesEnum pStrategy) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    description = pDescription;
    strategy = Optional.of(pStrategy);
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getCode() {
    return "";
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.BlankEdge;
  }

  @Override
  public <R, X extends Exception> R accept(CCfaEdgeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public BlankEdge copyWith(CFANode pNewPredecessorNode, CFANode pNewSuccessorNode) {
    return new BlankEdge(
        getRawStatement(),
        getFileLocation(),
        pNewPredecessorNode,
        pNewSuccessorNode,
        getDescription());
  }

  public Optional<StrategiesEnum> getStrategy() {
    return strategy;
  }
}
