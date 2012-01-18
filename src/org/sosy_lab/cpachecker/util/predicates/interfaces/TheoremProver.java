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
package org.sosy_lab.cpachecker.util.predicates.interfaces;

import java.util.Collection;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.Model;

public interface TheoremProver {

  void init();
  void push(Formula f);
  void pop();
  boolean isUnsat();
  boolean isUnsat(Formula f);
  Model getModel();
  void reset();

  AllSatResult allSat(Formula f, Collection<Formula> important,
                      AbstractionManager mgr, Timer timer);

  interface AllSatResult {

    /**
     * The result of an allSat call as an abstract formula.
     */
    public Region getResult();

    /**
     * The number of satisfying assignments contained in the result, of
     * {@link Integer#MAX_VALUE} if this number is infinite.
     */
    public int getCount();
  }
}
