// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains utility classes for program slicing.
 *
 * @see org.sosy_lab.cpachecker.util.dependencegraph
 */
package org.sosy_lab.cpachecker.util.smg;

import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.graph.SMGExplicitValue;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/**
 * Class implementing join algorithm from FIT-TR-2013-4 (Appendix C)
 */
public class SMGJoin {

  private SMG smg1;
  private SMG smg2;

  private SMGJoinStatus status = SMGJoinStatus.EQUAL;

  public SMGJoin(SMG pSmg1, SMG pSmg2) {
    smg1 = pSmg1;
    smg2 = pSmg2;
    joinSubSMGs();
    joinValues();
    joinTargetObjects();
    insertLeftDlsAndJoin();
    insertRightDlsAndJoin();

  }

  private void joinSubSMGs() {

  }

  private void joinValues() {

  }

  private void joinTargetObjects() {

  }

  private void insertRightDlsAndJoin() {

  }

  private void insertLeftDlsAndJoin() {

  }

  /**
   * Implementation of Algorithm 3.
   * @param obj1 - SMGObject of smg1
   * @param obj2 - SMGObject of smg2
   */
  private void joinFields(SMGObject obj1, SMGObject obj2) {
    Preconditions.checkArgument(
        obj1.getSize().equals(obj2.getSize()),
        "SMG fields with different sizes cannot be joined.");
    Preconditions.checkArgument( smg1.getObjects().contains(obj1) &&
        smg2.getObjects().contains(obj2),
        "Only objects of givens SMGs can be joined.");


    Set<SMGHasValueEdge> obj1Edges = processHasValueEdgeSet(obj1, obj2, smg1, smg2);
    Set<SMGHasValueEdge> obj2Edges = processHasValueEdgeSet(obj2, obj1, smg2, smg1);

  }

  /**
   * Implementation of Algorithm 3 step 2.
   * @param obj1 - SMGObject of smg1
   * @param obj2 - SMGObject of smg2
   */
  private Set<SMGHasValueEdge> processHasValueEdgeSet(SMGObject obj1, SMGObject obj2, SMG pSmg1, SMG pSmg2){
    //H1 and H2
    Set<SMGHasValueEdge> obj1Edges = pSmg1.getEdges(obj1);
    Set<SMGHasValueEdge> obj2Edges = pSmg2.getEdges(obj2);

    //2a)
    Set<SMGHasValueEdge> edgesObj1Without0Address =
        obj1Edges.stream().filter(edge -> !edge.hasValue().equals(SMGExplicitValue.nullInstance()))
            .collect(Collectors.toSet());
    return edgesObj1Without0Address;
  }

}
