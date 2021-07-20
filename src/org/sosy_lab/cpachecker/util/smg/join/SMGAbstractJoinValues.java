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
package org.sosy_lab.cpachecker.util.smg.join;

import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * Class implementing join algorithm from FIT-TR-2013-4 (Appendix C)
 */
public class SMGAbstractJoinValues {


  protected SMGJoinStatus status;
  protected SMG inputSMG1;
  protected SMG inputSMG2;
  protected SMG destSMG;
  protected SMGValue value;
  protected NodeMapping mapping1;
  protected NodeMapping mapping2;
  protected boolean isRecoverable = true;
  protected boolean isDefined = true;

  public SMGAbstractJoinValues(
      SMGJoinStatus pStatus,
      SMG pInputSMG1,
      SMG pInputSMG2,
      SMG pDestSMG,
      NodeMapping pMapping1,
      NodeMapping pMapping2) {
    status = pStatus;
    inputSMG1 = pInputSMG1;
    inputSMG2 = pInputSMG2;
    destSMG = pDestSMG;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
  }

  public NodeMapping getMapping1() {
    return mapping1;
  }

  public NodeMapping getMapping2() {
    return mapping2;
  }

  protected void setBottomState() {
    status = SMGJoinStatus.INCOMPARABLE;
    isDefined = false;
    isRecoverable = false;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public boolean isDefined() {
    return isDefined;
  }

  public boolean isRecoverable() {
    return isRecoverable;
  }

  public SMG getInputSMG1() {
    return inputSMG1;
  }

  public SMG getDestinationSMG() {
    return destSMG;
  }

  public SMG getInputSMG2() {
    return inputSMG2;
  }

  public SMGValue getValue() {
    return value;
  }

}
