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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

/**
 * Implementation of a property checker which does not accept abstract states representing some kind of "target" or
 * "error" abstract state. Accepts every abstract state which is not a target abstract state and every set of
 * states which does not contain a target abstract state.
 */
public class NoTargetStateChecker extends PerElementPropertyChecker {

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck) throws UnsupportedOperationException {
    return (!(pElemToCheck instanceof Targetable) || !((Targetable) pElemToCheck).isTarget());
  }

}
