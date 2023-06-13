// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractListCandidateSequenceBlock;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;

public class SMGSingleLinkedListCandidateSequenceBlock
    extends SMGAbstractListCandidateSequenceBlock<SMGSingleLinkedListShape> {

  public SMGSingleLinkedListCandidateSequenceBlock(
      SMGSingleLinkedListShape pShape, int pLength, SMGMemoryPath pPointerToStartObject) {
    super(pShape, pLength, pPointerToStartObject);
  }

  @Override
  public boolean isBlocked(SMGAbstractionCandidate pCandidate, UnmodifiableCLangSMG smg) {
    return pCandidate instanceof SMGSingleLinkedListCandidateSequence
        && super.isBlocked(pCandidate, smg);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SMGSingleLinkedListCandidateSequenceBlock && super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode(); // equals() in this class checks nothing more
  }
}
