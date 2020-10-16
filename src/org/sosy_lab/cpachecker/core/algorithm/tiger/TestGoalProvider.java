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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.CFAGoal;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class TestGoalProvider {
  Map<String, Set<CFAGoal>> cache;
  static TestGoalProvider provider;
  boolean reduceGoals;

  private static final String goalPrefix = "Goals:";
  private static final String goalRegex = "GoalRegex:";
  LogManager logger;

  public static TestGoalProvider getInstace(LogManager pLogger, boolean reduceGoals)
  {
    if (provider == null) {
      provider = new TestGoalProvider(pLogger, reduceGoals);
    }
    return provider;
  }

  public static TestGoalProvider getInstaceOrNull() {
    return provider;
  }

  private TestGoalProvider(LogManager pLogger, boolean reduceGoals) {
    cache = new HashMap<>();
    logger = pLogger;
    this.reduceGoals = reduceGoals;
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
    } else {
      Pattern pattern = Pattern.compile("COVER EDGES\\(@CALL\\((.*)\\)\\)");
      Matcher matcher = pattern.matcher(fqlQuery);
      if (matcher.find()) {
        String functionName = matcher.group(1);
        edgeCriterion =
            edge -> edge instanceof CStatementEdge
                && ((CStatementEdge) edge).getStatement() instanceof CFunctionCall
                && ((CFunctionCall) ((CStatementEdge) edge).getStatement())
                    .getFunctionCallExpression()
                    .getFunctionNameExpression()
                    .toASTString()
                    .equals(functionName);
      }

    }

    if (edgeCriterion != null) {
      Set<CFAEdge> edges = extractEdgesByCriterion(edgeCriterion, cfa);

      if (reduceGoals) {
        edges = reduceSimpleGoals(edges);
      }

      Set<CFAGoal> goals = new HashSet<>();
      for (CFAEdge edge : edges) {
        goals.add(new CFAGoal(edge));
      }
      return goals;
    }
    return null;
  }

  private boolean hasSuccessorGoal(CFAEdge goal, Set<CFAEdge> goals, int maxdepth) {
    if (maxdepth <= 0) {
      return false;
    }
    CFANode sucessor = goal.getSuccessor();
    if (sucessor.getNumEnteringEdges() != 1) {
      return false;
    }
    for (CFAEdge leaving : CFAUtils.leavingEdges(sucessor)) {
      if (!goals.contains(leaving)) {
        maxdepth--;
        if (!hasSuccessorGoal(leaving, goals, maxdepth)) {
          return false;
        }
      }
    }
    return true;
  }

  public Set<CFAEdge> reduceSimpleGoals(final Set<CFAEdge> goals) {
    Set<CFAEdge> newGoals = new HashSet<>(goals);
    for (CFAEdge goal : goals) {
      boolean redundantGoal = hasSuccessorGoal(goal, newGoals, 50);
        if (redundantGoal) {
        newGoals.remove(goal);
        }
    }
    return newGoals;
  }


  private Set<CFAGoal> extractGoalsFromList(String fqlQuery, CFA cfa) {
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

  private Set<CFAGoal> extractGoalFromRegex(String fqlQuery, CFA cfa) {
    String query = fqlQuery.substring(goalRegex.length());
    Pattern pattern = Pattern.compile(query);
    Set<CFAEdge> edges = new HashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      edges.addAll(CFAUtils.allLeavingEdges(node).toSet());
    }
    Set<CFAGoal> cfaGoals = new HashSet<>();
    for (CFAEdge edge : edges) {
      Matcher matcher = pattern.matcher(edge.getDescription());
      if (matcher.find()) {
        cfaGoals.add(new CFAGoal(edge));
      }
    }
    return cfaGoals;
  }

  private Set<CFAGoal> extractGoalSyntax(String fqlQuery, CFA cfa) {
    if (fqlQuery.startsWith(goalPrefix)) {
      return extractGoalsFromList(fqlQuery, cfa);
    }
    if (fqlQuery.startsWith(goalRegex)) {
      return extractGoalFromRegex(fqlQuery, cfa);
    }

    throw new RuntimeException("Could not parse FQL Query: " + fqlQuery);

  }

  public Set<CFAGoal> initializeTestGoalSet(
      String fqlQuery,
      CFA cfa,
      @SuppressWarnings("unused") boolean reduceGoals) {
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
