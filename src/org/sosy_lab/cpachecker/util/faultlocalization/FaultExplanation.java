/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.faultlocalization;

import org.sosy_lab.cpachecker.util.faultlocalization.ranking.NoContextExplanation;

/**
 * Every reason needs a description.
 * If there is a similar way to map Faults to a description a FaultExplanation can be created.
 * After processing a Fault it returns a String on why this Fault leads to an error.
 */
@FunctionalInterface
public interface FaultExplanation {

  /**
   * Map a set of FaultContributions to an explanation string.
   * This string can be used by RankInfo as a description.
   * For an example see NoContextExplanation.
   * @param subset set to find a reason for
   * @return explanation as string
   * @see NoContextExplanation
   */
  String explanationFor(Fault subset);
}
