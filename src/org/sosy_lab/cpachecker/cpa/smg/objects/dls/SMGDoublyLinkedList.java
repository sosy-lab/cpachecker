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
package org.sosy_lab.cpachecker.cpa.smg.objects.dls;

import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;


/**
 *
 */
public class SMGDoublyLinkedList extends SMGObject implements SMGAbstractObject {

  private final int minimumLength;

  private final int hfo;
  private final int nfo;
  private final int pfo;

  public SMGDoublyLinkedList(int pSize, int pHfo, int pNfo, int pPfo,
      int pMinLength, int level) {
    super(pSize, "__dls +" + pMinLength, level);

    hfo = pHfo;
    nfo = pNfo;
    pfo = pPfo;
    minimumLength = pMinLength;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject#matchGenericShape(org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject)
   */
  @Override
  public boolean matchGenericShape(SMGAbstractObject pOther) {
    return false;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject#matchSpecificShape(org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject)
   */
  @Override
  public boolean matchSpecificShape(SMGAbstractObject pOther) {
    return false;
  }

  public int getMinimumLength() {
    return minimumLength;
  }

  public int getHfo() {
    return hfo;
  }

  public int getNfo() {
    return nfo;
  }

  public int getPfo() {
    return pfo;
  }
}