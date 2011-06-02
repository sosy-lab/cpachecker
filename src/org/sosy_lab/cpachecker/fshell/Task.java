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
package org.sosy_lab.cpachecker.fshell;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public class Task implements Iterable<ElementaryCoveragePattern> {

  private Collection<ElementaryCoveragePattern> mTestGoals;
  private ElementaryCoveragePattern mPassingClause;

  public Task(Collection<ElementaryCoveragePattern> pTestGoals) {
    mTestGoals = pTestGoals;
    mPassingClause = null;
  }

  public Task(Collection<ElementaryCoveragePattern> pTestGoals, ElementaryCoveragePattern pPassingClause) {
    this(pTestGoals);
    mPassingClause = pPassingClause;
  }

  public int getNumberOfTestGoals() {
    return mTestGoals.size();
  }

  public boolean hasPassingClause() {
    return (mPassingClause != null);
  }

  @Override
  public Iterator<ElementaryCoveragePattern> iterator() {
    return mTestGoals.iterator();
  }

  public ElementaryCoveragePattern getPassingClause() {
    return mPassingClause;
  }

  public static Task create(FQLSpecification pSpecification, CFANode pInitialNode) {
    return create(pSpecification, new CoverageSpecificationTranslator(pInitialNode));
  }

  public static Task create(FQLSpecification pSpecification, CoverageSpecificationTranslator pCoverageSpecificationTranslator) {
    Collection<ElementaryCoveragePattern> lGoals = pCoverageSpecificationTranslator.translate(pSpecification.getCoverageSpecification());

    if (pSpecification.hasPassingClause()) {
      ElementaryCoveragePattern lPassing = pCoverageSpecificationTranslator.translate(pSpecification.getPathPattern());

      return new Task(lGoals, lPassing);
    }
    else {
      return new Task(lGoals);
    }
  }

  public Deque<Goal> toGoals(GuardedEdgeLabel pAlphaLabel, GuardedEdgeLabel pInverseAlphaLabel, GuardedEdgeLabel pOmegaLabel) {
    LinkedList<Goal> lGoals = new LinkedList<Goal>();

    for (ElementaryCoveragePattern lGoalPattern : this) {
      Goal lGoal = new Goal(lGoalPattern, pAlphaLabel, pInverseAlphaLabel, pOmegaLabel);
      lGoals.add(lGoal);
    }

    return lGoals;
  }

}
