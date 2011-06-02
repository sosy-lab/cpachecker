package org.sosy_lab.cpachecker.util.ecp.translators;

import java.util.Collections;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;

public class AllCFAEdgesGuardedEdgeLabel extends GuardedEdgeLabel {

  private static AllCFAEdgesGuardedEdgeLabel sInstance = new AllCFAEdgesGuardedEdgeLabel();

  public static AllCFAEdgesGuardedEdgeLabel getInstance() {
    return sInstance;
  }

  private AllCFAEdgesGuardedEdgeLabel() {
    super(new ECPEdgeSet(Collections.<CFAEdge>emptySet()));
  }

  @Override
  public ECPEdgeSet getEdgeSet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(CFAEdge pCFAEdge) {
    return true;
  }

  @Override
  public boolean equals(Object pOther) {
    return (this == sInstance);
  }

}
