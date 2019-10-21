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

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.CFAGoal;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class TestGoalProvider {
  Map<String, Set<CFAGoal>> cache;
  static TestGoalProvider provider;


  private final String goalPrefix = "Goals:";

  LogManager logger;

  public static TestGoalProvider getInstace(LogManager pLogger)
  {
    if (provider == null) {
      provider = new TestGoalProvider(pLogger);
    }
    return provider;
  }

  private TestGoalProvider(LogManager pLogger) {
    cache = new HashMap<>();
    logger = pLogger;
  }

  private Predicate<CFAEdge> getStatementCriterion() {
    return edge -> edge.getEdgeType() == CFAEdgeType.DeclarationEdge
        || edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge
        || edge.getEdgeType() == CFAEdgeType.StatementEdge;
  }

  private Predicate<CFAEdge> getErrorCriterion() {
    return edge -> edge instanceof CStatementEdge
        && ((CStatementEdge) edge).getStatement() instanceof CFunctionCall
        && ((CFunctionCall) ((CStatementEdge) edge).getStatement()).getFunctionCallExpression()
            .getFunctionNameExpression()
            .toASTString()
            .equals("__VERIFIER_error");
  }

  private Predicate<CFAEdge> getAssumeEdgeCriterion() {
    return edge -> edge instanceof AssumeEdge;
  }

  private Set<CFAEdge>
      extractEdgesByCriterion(final Predicate<CFAEdge> criterion, CFA cfa, String fqlQuery) {
    Set<CFAEdge> edges = new HashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      edges.addAll(CFAUtils.allLeavingEdges(node).filter(criterion).toSet());
    }
    return edges;
  }

  private Set<CFAGoal> tryExtractPredefinedFQL(String fqlQuery, CFA cfa) {
    // check if its an predefined FQL Statement
    Predicate<CFAEdge> edgeCriterion = null;
    logger.log(Level.INFO, "trying to extract predefinedFQL: " + fqlQuery);
    if (fqlQuery.equalsIgnoreCase(PredefinedCoverageCriteria.StatementCoverage)) {
      edgeCriterion = getStatementCriterion();
    } else if (fqlQuery.equalsIgnoreCase(PredefinedCoverageCriteria.ErrorCoverage)) {
      edgeCriterion = getErrorCriterion();
    } else if (fqlQuery.equalsIgnoreCase(PredefinedCoverageCriteria.DecisionCoverage)
        || fqlQuery.equalsIgnoreCase(PredefinedCoverageCriteria.ConditionCoverage)
        || fqlQuery.equalsIgnoreCase(PredefinedCoverageCriteria.AssumeCoverage)) {
      edgeCriterion = getAssumeEdgeCriterion();
    }
    if (edgeCriterion != null) {
      Set<CFAEdge> edges = extractEdgesByCriterion(edgeCriterion, cfa, fqlQuery);
      Set<CFAGoal> goals = new HashSet<>();
      for (CFAEdge edge : edges) {
        goals.add(new CFAGoal(edge));
      }
      return goals;
    }
    return null;
  }

  private void reduceGoals(Set<CFAGoal> goals) {
    // TODO only for test-comp remove afterwards
    Set<CFAGoal> keptGoals = new HashSet<>(goals);
    boolean allSuccessorsGoals;
    for (CFAGoal goal : goals) {
      if (goal.getCFAEdgesGoal().getEdges().size() != 1) {
        continue;
      }
      CFAEdge edge = goal.getCFAEdgesGoal().getEdges().get(0);
      if (edge.getSuccessor().getNumEnteringEdges() == 1) {
        allSuccessorsGoals = true;
        for (CFAEdge leaving : CFAUtils.leavingEdges(edge.getSuccessor())) {
          if (!keptGoals.stream()
              .filter(g -> g.getCFAEdgesGoal().getEdges().get(0) == leaving)
              .findFirst()
              .isPresent()) {
            allSuccessorsGoals = false;
            break;
          }
        }
        if (allSuccessorsGoals) {
          keptGoals.remove(goal);
        }
      }
    }
    goals.clear();
    goals.addAll(keptGoals);
  }

  private Set<CFAGoal> extractGoalSyntax(String fqlQuery, CFA cfa) {
    if (!fqlQuery.startsWith(goalPrefix)) {
      throw new RuntimeException("Could not parse FQL Query: " + fqlQuery);
    }
    String query = fqlQuery.substring(goalPrefix.length());
    String[] goals = query.split(",");
    Set<CFAEdge> edges = new HashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      edges.addAll(CFAUtils.allLeavingEdges(node).toSet());
    }
    Set<CFAGoal> cfaGoals = new HashSet<>();
    for (String goal : goals) {
      String[] edgeLabels = goal.split("->");
      List<CFAEdge> goalEdges = new ArrayList<>();
      for (String edgeLabel : edgeLabels) {
        String label = "Label: " + edgeLabel.trim();
        for (CFAEdge edge : edges) {
          if (edge.getDescription().contains(label)) {
            goalEdges.add(edge);
            break;
          }
        }
      }
      if (goalEdges.size() >= 1) {
        cfaGoals.add(new CFAGoal(goalEdges));
      }
    }
    return cfaGoals;
  }

  public Set<CFAGoal> initializeTestGoalSet(String fqlQuery, CFA cfa) {
    if (!cache.containsKey(fqlQuery)) {
      Set<CFAGoal> goals = tryExtractPredefinedFQL(fqlQuery, cfa);
      if (goals == null) {
        goals = extractGoalSyntax(fqlQuery, cfa);
      }
      // TODO reduce goals for variable output might be wrong?
      reduceGoals(goals);
      cache.put(fqlQuery, goals);

    }
    return new HashSet<>(cache.get(fqlQuery));

  }

}
