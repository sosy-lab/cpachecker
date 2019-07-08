/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util.slicing;

import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.Specification;

public interface SlicingCriteriaExtractor {

  /**
   * Extracts from <code>pCfa</code> a set of slicing criteria, i.e., a subset of the CFA's edges.
   * The slicing criteria depends on the type of extractor, the program, and the property <code>
   * pError</code>.
   *
   * @param pCfa program for which slicing criteria should be extracted
   * @param pError specification of non-reachability property
   * @param pShutdownNotifier notifies whether a shutdown was requested and extraction of slicing
   *     criteria should be aborted
   * @param pLogger object for writing log messages
   * @return subset of CFA edges
   * @throws InterruptedException if a shutdown is requested and this is detected with the help of
   *     the <code>pShutdownNotifier</code>
   */
  public Set<CFAEdge> getSlicingCriteria(
      CFA pCfa, Specification pError, ShutdownNotifier pShutdownNotifier, LogManager pLogger)
      throws InterruptedException;
}
