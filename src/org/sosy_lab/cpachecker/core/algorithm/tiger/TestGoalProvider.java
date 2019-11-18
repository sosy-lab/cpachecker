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
import java.util.Iterator;
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


  private static final String goalPrefix = "Goals:";

  LogManager logger;

  public static TestGoalProvider getInstace(LogManager pLogger)
  {
    if (provider == null) {
      provider = new TestGoalProvider(pLogger);
    }
    return provider;
  }

  public static TestGoalProvider getInstaceOrNull() {
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
      extractEdgesByCriterion(final Predicate<CFAEdge> criterion, CFA cfa) {
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
      Set<CFAEdge> edges = extractEdgesByCriterion(edgeCriterion, cfa);

      edges = adaptTestTargets(edges);

      Set<CFAGoal> goals = new HashSet<>();
      for (CFAEdge edge : edges) {
        goals.add(new CFAGoal(edge));
      }
      return goals;
    }
    return null;
  }

  private void removeEdgeGoal(Set<CFAGoal> goals, CFAEdge edge) {
    Iterator<CFAGoal> iter = goals.iterator();
    while (iter.hasNext()) {
      CFAGoal goal = iter.next();
      if (goal.getCFAEdgesGoal().getEdges().size() == 1
          && goal.getCFAEdgesGoal().getEdges().get(0).equals(edge)) {
        iter.remove();
      }
    }
  }

  private void reduceGoals(Set<CFAGoal> goals) {
    for (CFAGoal goal : goals) {
      if (goal.getCFAEdgesGoal().getEdges().size() != 1) {
        // make sure each goals has only 1 edge
        return;
      }
    }
    Set<CFAEdge> goalEdges = new HashSet<>();
    for (CFAGoal goal : goals) {
      goalEdges.add(goal.getCFAEdgesGoal().getEdges().get(0));
    }

    for (CFAEdge edge : goalEdges) {
      while (edge.getSuccessor().getNumLeavingEdges() == 1) {
        edge = edge.getSuccessor().getLeavingEdge(0);
        removeEdgeGoal(goals, edge);
      }
    }
  }

  public Set<CFAEdge> COVERED_NEXT_EDGEadaptTestTargets(final Set<CFAEdge> targets) {
    // currently only simple heuristic
    Set<CFAEdge> newGoals;
    newGoals = new HashSet<>(targets);
    boolean allSuccessorsGoals;
    for (CFAEdge target : targets) {
      if (target.getSuccessor().getNumEnteringEdges() == 1) {
        allSuccessorsGoals = true;
        for (CFAEdge leaving : CFAUtils.leavingEdges(target.getSuccessor())) {
          if (!targets.contains(leaving)) {
            allSuccessorsGoals = false;
            break;
          }
        }
        if (allSuccessorsGoals) {
          newGoals.remove(target);
        }
      }
    }
    return newGoals;
  }

  public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets) {
    // currently only simple heuristic
    Set<CFAEdge> newGoals;
    if (targets.size() < 1000) {
      newGoals = COVERED_NEXT_EDGEadaptTestTargets(targets);
    } else {
      newGoals = new HashSet<>();
      for (CFAEdge target : targets) {
        if (target.getEdgeType() == CFAEdgeType.AssumeEdge) {
          for (CFAEdge leaving : CFAUtils.leavingEdges(target.getSuccessor())) {
            if (!(leaving.getEdgeType() == CFAEdgeType.AssumeEdge)) {
              newGoals.add(leaving);
            }
          }
        } else {
          newGoals.add(target);
        }
      }
    }
    return newGoals;
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

  public Set<CFAGoal> initializeTestGoalSet(String fqlQuery, CFA cfa, boolean reduceGoals) {
    if (!cache.containsKey(fqlQuery)) {
      Set<CFAGoal> goals = tryExtractPredefinedFQL(fqlQuery, cfa);
      if (goals == null) {
        goals = extractGoalSyntax(fqlQuery, cfa);
      }
      // TODO reduce goals for variable output might be wrong?
      // if(reduceGoals) {
      // logger.log(Level.INFO, "Number of goals before reduction:" + goals.size());
      // reduceGoals(goals);
      // logger.log(Level.INFO, "Number of goals after reduction:" + goals.size());
      // }
      cache.put(fqlQuery, goals);

    }

    return new HashSet<>(cache.get(fqlQuery));
  }

}
