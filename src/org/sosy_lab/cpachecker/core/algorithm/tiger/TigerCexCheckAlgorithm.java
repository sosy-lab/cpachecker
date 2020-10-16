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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.CFAGoal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.multigoal.MultiGoalCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TigerCexCheckAlgorithm implements Algorithm, ShutdownRequestListener {
  CFA cfa;
  TestSuite<CFAGoal> testsuite;
  Algorithm mainAlgo;
  ConfigurableProgramAnalysis cpa;
  String property;
  LogManager logger;

  public TigerCexCheckAlgorithm(
      Algorithm pMainAlgo,
      ConfigurableProgramAnalysis pCPA,
      CFA pCFA,
      Specification pSpecification,
      LogManager pLogger) {

    mainAlgo = pMainAlgo;
    cpa = pCPA;
    cfa = pCFA;
    logger = pLogger;
    getPropertyFromSpec(pSpecification);

  }

  private void getPropertyFromSpec(Specification spec) {
    assert spec.getPathToSpecificationAutomata().keySet().size() == 1;
    for (Path specPath : spec.getPathToSpecificationAutomata().keySet()) {
      try {
        String content = new String(Files.readAllBytes(specPath), StandardCharsets.UTF_8);

        Pattern pattern =
            Pattern.compile("(<data key=\"violatedProperty\">)(((?!.*</data>).*\\n*)*)");
        Matcher matcher = pattern.matcher(content);
        matcher.find();
        property =
            matcher.group(2)
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&apos;", "'");
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage());
      }

    }

  }

  private void reduceGoalsToProperty(Set<CFAGoal> goalsToCover) {
    Iterator<CFAGoal> iter = goalsToCover.iterator();
    while (iter.hasNext()) {
      CFAGoal goal = iter.next();
      if (!goal.getCFAEdgesGoal().toString().equals(property)) {
        iter.remove();
      } else {
        logger.log(Level.INFO, "Cex Check for Goal: " + goal.toString());
      }
    }
    assert goalsToCover.size() == 1;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    testsuite = TestSuite.getCFAGoalTSOrNull();
    Set<CFAGoal> goalsToCover = testsuite.getUncoveredGoals();

    reduceGoalsToProperty(goalsToCover);

    if (goalsToCover.isEmpty()) {
      pReachedSet.clear();
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    MultiGoalCPA multiGoalCPA = null;
    if (cpa instanceof WrapperCPA) {
      multiGoalCPA = ((WrapperCPA) cpa).retrieveWrappedCpa(MultiGoalCPA.class);
    } else if (cpa instanceof MultiGoalCPA) {
      multiGoalCPA = ((MultiGoalCPA) cpa);
    }
    multiGoalCPA.setTransferRelationTargets(
        goalsToCover.stream().map(goal -> goal.getCFAEdgesGoal()).collect(Collectors.toSet()));

    return mainAlgo.run(pReachedSet);
  }

  @Override
  public void shutdownRequested(String pArg0) {
    // TODO Auto-generated method stub

  }

}
