package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract class SummaryEdge extends AbstractCFAEdge {

  public SummaryEdge(String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
  }

}
