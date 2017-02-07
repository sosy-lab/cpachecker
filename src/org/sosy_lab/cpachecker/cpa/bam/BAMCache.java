/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;


public interface BAMCache extends Statistics {

  void put(AbstractState stateKey, Precision precisionKey, Block context, ReachedSet item);

  void put(AbstractState stateKey, Precision precisionKey, Block context,
      Collection<AbstractState> item,
      ARGState rootOfBlock);

  void remove(AbstractState stateKey, Precision precisionKey, Block context);

  /**
   * This function returns a Pair of the reached-set and the returnStates for the given keys.
   * Both members of the returned Pair are NULL, if there is a cache miss.
   * For a partial cache hit we return the partly computed reached-set and NULL as returnStates. */
  Pair<ReachedSet, Collection<AbstractState>> get(
      AbstractState stateKey,
      Precision precisionKey,
      Block context);

  ARGState getLastAnalyzedBlock();

  boolean containsPreciseKey(AbstractState stateKey, Precision precisionKey, Block context);

  Collection<ReachedSet> getAllCachedReachedStates();
}