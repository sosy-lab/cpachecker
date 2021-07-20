// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * Class implementing join algorithm from FIT-TR-2013-4 (Appendix C.3)
 */
public class SMGJoinValues {

  private SMGJoinStatus status;
  private SMG inputSMG1;
  private SMG inputSMG2;
  private SMG destSMG;
  private SMGValue value;
  private NodeMapping mapping1;
  private NodeMapping mapping2;


  public SMGJoinValues(
      SMGJoinStatus pStatus,
      SMG pInputSMG1,
      SMG pInputSMG2,
      SMG pDestSMG,
      NodeMapping pMapping1,
      NodeMapping pMapping2,
      SMGValue pValue1,
      SMGValue pValue2,
      int pNewNestingLevelDiff) {
    // TODO Auto-generated constructor stub
  }

  /**
   * Implementation of Algorithm 5.
   *
   * @param obj1 - SMGObject of smg1
   * @param obj2 - SMGObject of smg2
   */
  public void joinValues() {

  }

  public SMGJoinStatus getStatus() {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isDefined() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean isRecoverable() {
    // TODO Auto-generated method stub
    return false;
  }

  public SMG getInputSMG1() {
    // TODO Auto-generated method stub
    return null;
  }

  public SMG getDestinationSMG() {
    // TODO Auto-generated method stub
    return null;
  }

  public SMG getInputSMG2() {
    // TODO Auto-generated method stub
    return null;
  }

  public SMGValue getValue() {
    // TODO Auto-generated method stub
    return null;
  }

}
