/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractListCandidateSequenceBlock;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;

public class SMGDoublyLinkedListCandidateSequenceBlock
    extends SMGAbstractListCandidateSequenceBlock<SMGDoublyLinkedListShape> {

  public SMGDoublyLinkedListCandidateSequenceBlock(SMGDoublyLinkedListShape pShape, int pLength,
      SMGMemoryPath pPointerToStartObject) {
    super(pShape, pLength, pPointerToStartObject);
  }

  @Override
  public boolean isBlocked(SMGAbstractionCandidate pCandidate, UnmodifiableCLangSMG smg) {
    return pCandidate instanceof SMGDoublyLinkedListCandidateSequence && super.isBlocked(pCandidate, smg);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SMGDoublyLinkedListCandidateSequenceBlock && super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode(); // equals() in this class checks nothing more
  }
}