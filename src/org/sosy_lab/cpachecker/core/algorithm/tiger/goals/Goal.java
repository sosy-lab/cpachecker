/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.goals;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public abstract class Goal {


  protected int mIndex;
  protected Region mPresenceCondition;


  protected void init(int pIndex, Region pPresenceCondition) {
    mIndex = pIndex;
    mPresenceCondition = pPresenceCondition;
  }

  public int getIndex() {
    return mIndex;
  }

  @Override
  public String toString() {
    return getName();
  }

  public abstract String getName();


  public Region getPresenceCondition() {
    return mPresenceCondition;
  }

  public void setPresenceCondition(Region pPresenceCondition) {
    mPresenceCondition = pPresenceCondition;
  }

  public abstract ThreeValuedAnswer getsCoveredByPath(List<CFAEdge> pPath);
}
