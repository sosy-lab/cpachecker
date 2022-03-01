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

  public SMG getSmg1() {
    return smg1;
  }

  public SMG getSmg2() {
    return smg2;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

}
