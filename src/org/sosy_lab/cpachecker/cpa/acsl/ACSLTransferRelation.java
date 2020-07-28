// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotationLocations;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.core.algorithm.acsl.ACSLTermToCExpressionVisitor;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ACSLTransferRelation extends SingleEdgeTransferRelation {

  private final CFAWithACSLAnnotationLocations cfa;
  private final ACSLTermToCExpressionVisitor visitor;

  public ACSLTransferRelation(
      CFAWithACSLAnnotationLocations pCFA, ACSLTermToCExpressionVisitor pVisitor) {
    cfa = pCFA;
    visitor = pVisitor;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    Set<ACSLAnnotation> annotationsForState =
        ImmutableSet.copyOf(cfa.getEdgesToAnnotations().get(cfaEdge));
    return ImmutableList.of(new ACSLState(annotationsForState, visitor));
  }
}
