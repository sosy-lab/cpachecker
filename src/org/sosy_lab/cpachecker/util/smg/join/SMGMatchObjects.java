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
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/**
 * Class implementing join algorithm from FIT-TR-2013-4 (Appendix C.4) Algorithm 8.
 *
 */
public class SMGMatchObjects extends SMGAbstractJoin {


  @SuppressWarnings("unused")
  public SMGMatchObjects(
      SMGJoinStatus pStatus,
      SMG pInputSMG1,
      SMG pInputSMG2,
      SMG pDestSMG,
      NodeMapping pMapping1,
      NodeMapping pMapping2,
      SMGObject pTargetObject1,
      SMGObject pTargetObject2) {
    super(pStatus, pInputSMG1, pInputSMG2, pDestSMG, pMapping1, pMapping2);
  }


}
