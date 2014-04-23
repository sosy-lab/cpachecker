/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smgfork.join;

import java.util.Collections;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cpa.smgfork.SMG;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGObject;


final public class SMGJoinSubSMGsForAbstraction {
  private SMGJoinStatus status = null;
  final private SMG resultSMG = null;
  final private SMGObject newAbstractObject = null;
  final private Pair<Set<SMGObject>, Set<Integer>> nonSharedFromSMG1 = null;
  final private Pair<Set<SMGObject>, Set<Integer>> nonSharedFromSMG2= null;
  private boolean defined = false;

  public SMGJoinSubSMGsForAbstraction(SMG pSMG, SMGObject pObj1, SMGObject pObj2) {

  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getResultSMG() {
    return resultSMG;
  }

  public SMGObject getNewAbstractObject() {
    return newAbstractObject;
  }

  public Set<SMGObject> getNonSharedObjectsFromSMG1() {
    return Collections.unmodifiableSet(nonSharedFromSMG1.getFirst());
  }

  public Set <Integer> getNonSharedValuesFromSMG1() {
    return Collections.unmodifiableSet(nonSharedFromSMG1.getSecond());
  }

  public Set<SMGObject> getNonSharedObjectsFromSMG2() {
    return Collections.unmodifiableSet(nonSharedFromSMG2.getFirst());
  }

  public Set<Integer> getNonSharedValuesFromSMG2() {
    return Collections.unmodifiableSet(nonSharedFromSMG2.getSecond());
  }
}
