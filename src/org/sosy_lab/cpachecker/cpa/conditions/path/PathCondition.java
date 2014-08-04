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
package org.sosy_lab.cpachecker.cpa.conditions.path;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;

/**
 * Interface for a specific class of conditions which limit single paths
 * depending on some condition like its length.
 * Implementations of this interface can be used with the {@link PathConditionsCPA}.
 * For this to work, they need to have a public constructor with two parameters
 * of types {@link org.sosy_lab.common.configuration.Configuration} and
 * {@link org.sosy_lab.common.log.LogManager}, respectively.
 *
 * In order to cut off a path, conditions need to return an element from
 * {@link PathCondition#getAbstractSuccessor(AbstractState, CFAEdge)} whose
 * {@link AvoidanceReportingState#mustDumpAssumptionForAvoidance()} method
 * returns true.
 * Note that this will have an effect only if the
 * {@link org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageCPA}
 * is present.
 */
public interface PathCondition {

  /**
   * Get the initial element.
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getInitialState(CFANode)
   */
  AvoidanceReportingState getInitialState(CFANode pNode);

  /**
   * Get the successor element for an edge.
   * @see org.sosy_lab.cpachecker.core.interfaces.TransferRelation#getAbstractSuccessors(AbstractState, org.sosy_lab.cpachecker.core.interfaces.Precision, CFAEdge)
   */
  AvoidanceReportingState getAbstractSuccessor(AbstractState pElement, CFAEdge pEdge);

  /**
   * Adjust the precision of this condition, i.e., by increasing a threshold value.
   * @see org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA#adjustPrecision()
   */
  boolean adjustPrecision();
}
