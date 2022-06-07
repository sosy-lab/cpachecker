// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cpa.invariants.EdgeAnalyzer;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class DataRaceTracker {

  private final ImmutableMap<String, Set<MemoryLocation>> accessedLocations;
  private final ImmutableMap<String, Set<MemoryLocation>> modifiedLocations;
  private final boolean hasDataRace;
  private final EdgeAnalyzer edgeAnalyzer;

  DataRaceTracker(EdgeAnalyzer pEdgeAnalyzer) {
    this(ImmutableMap.of(), ImmutableMap.of(), false, pEdgeAnalyzer);
  }

  private DataRaceTracker(
      Map<String, Set<MemoryLocation>> pAccessedLocations,
      Map<String, Set<MemoryLocation>> pModifiedLocations,
      boolean pHasDataRace,
      EdgeAnalyzer pEdgeAnalyzer) {
    accessedLocations = ImmutableMap.copyOf(pAccessedLocations);
    modifiedLocations = ImmutableMap.copyOf(pModifiedLocations);
    hasDataRace = pHasDataRace;
    edgeAnalyzer = pEdgeAnalyzer;
  }

  boolean hasDataRace() {
    return hasDataRace;
  }

  DataRaceTracker update(Set<String> threadIds, @Nullable String activeThread, CFAEdge edge) {
    Set<MemoryLocation> newlyAccessedLocations = getAccessedMemoryLocations(edge);
    Set<MemoryLocation> newlyModifiedLocations = getModifiedMemoryLocations(edge);

    boolean nextHasDataRace = hasDataRace;

    for (Entry<String, Set<MemoryLocation>> entry : accessedLocations.entrySet()) {
      if (nextHasDataRace) {
        break;
      }
      if (entry.getKey().equals(activeThread) || !threadIds.contains(entry.getKey())) {
        continue;
      }
      for (MemoryLocation accessed : entry.getValue()) {
        if (newlyModifiedLocations.contains(accessed)) {
          nextHasDataRace = true;
          break;
        }
      }
    }

    for (Entry<String, Set<MemoryLocation>> entry : modifiedLocations.entrySet()) {
      if (nextHasDataRace) {
        break;
      }
      if (entry.getKey().equals(activeThread) || !threadIds.contains(entry.getKey())) {
        continue;
      }
      for (MemoryLocation modified : entry.getValue()) {
        if (newlyAccessedLocations.contains(modified)) {
          nextHasDataRace = true;
          break;
        }
      }
    }

    assert accessedLocations.keySet().containsAll(modifiedLocations.keySet());
    assert modifiedLocations.keySet().containsAll(accessedLocations.keySet());

    Builder<String, Set<MemoryLocation>> accessedBuilder = new Builder<>();
    Builder<String, Set<MemoryLocation>> modifiedBuilder = new Builder<>();
    for (String threadId : threadIds) {
      Set<MemoryLocation> accessed;
      Set<MemoryLocation> modified;
      if (!accessedLocations.containsKey(threadId)) {
        accessed = new HashSet<>();
        modified = new HashSet<>();
      } else {
        accessed = new HashSet<>(accessedLocations.get(threadId));
        modified = new HashSet<>(modifiedLocations.get(threadId));
      }

      if (threadId.equals(activeThread)) {
        accessed.addAll(newlyAccessedLocations);
        modified.addAll(newlyModifiedLocations);
      }
      accessedBuilder.put(threadId, accessed);
      modifiedBuilder.put(threadId, modified);
    }
    return new DataRaceTracker(accessedBuilder.build(), modifiedBuilder.build(), nextHasDataRace,
        edgeAnalyzer);
  }

  private Set<MemoryLocation> getAccessedMemoryLocations(CFAEdge edge) {
    Set<MemoryLocation> accessedByEdge = new HashSet<>();
    switch (edge.getEdgeType()) {
      case AssumeEdge: {
        AssumeEdge assumeEdge = (AssumeEdge) edge;
        AExpression expression = assumeEdge.getExpression();
        return edgeAnalyzer.getInvolvedVariableTypes(expression, assumeEdge).keySet();
      }
      case DeclarationEdge:
        ADeclarationEdge declarationEdge = (ADeclarationEdge) edge;
        ADeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;
          MemoryLocation declaredVariable =
              MemoryLocation.fromQualifiedName(variableDeclaration.getQualifiedName());
          accessedByEdge.add(declaredVariable);
          CInitializer initializer = variableDeclaration.getInitializer();
          if (initializer != null) {
            accessedByEdge.addAll(
                edgeAnalyzer.getInvolvedVariableTypes(initializer, edge).keySet());
          }
        }
        return accessedByEdge;
      case FunctionCallEdge: {
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) edge;
        for (AExpression argument : functionCallEdge.getArguments()) {
          accessedByEdge.addAll(
              edgeAnalyzer.getInvolvedVariableTypes(argument, functionCallEdge).keySet());
        }
        return accessedByEdge;
      }
      case ReturnStatementEdge: {
        AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) edge;
        if (returnStatementEdge.getExpression().isPresent()) {
          AExpression returnExpression = returnStatementEdge.getExpression().get();
          accessedByEdge.addAll(
              edgeAnalyzer.getInvolvedVariableTypes(returnExpression, edge).keySet());
        }
        return accessedByEdge;
      }
      case StatementEdge: {
        AStatementEdge statementEdge = (AStatementEdge) edge;
        AStatement statement = statementEdge.getStatement();
        if (statement instanceof AExpressionAssignmentStatement) {
          accessedByEdge.addAll(
              edgeAnalyzer
                  .getInvolvedVariableTypes((AExpressionAssignmentStatement) statement, edge)
                  .keySet());
        } else if (statement instanceof AExpressionStatement) {
          accessedByEdge.addAll(
              edgeAnalyzer
                  .getInvolvedVariableTypes(
                      ((AExpressionStatement) statement).getExpression(), edge)
                  .keySet());
        } else if (statement instanceof AFunctionCallAssignmentStatement) {
          accessedByEdge.addAll(
              edgeAnalyzer
                  .getInvolvedVariableTypes((AFunctionCallAssignmentStatement) statement, edge)
                  .keySet());
        } else if (statement instanceof AFunctionCallStatement) {
          AFunctionCallStatement functionCallStatement = (AFunctionCallStatement) statement;
          for (AExpression expression :
              functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
            accessedByEdge.addAll(
                edgeAnalyzer.getInvolvedVariableTypes(expression, edge).keySet());
          }
        }
        return accessedByEdge;
      }
      case FunctionReturnEdge:
      case BlankEdge:
      case CallToReturnEdge:
        return accessedByEdge;
      default:
        throw new AssertionError("Unknown edge type: " + edge.getEdgeType());
    }
  }

  private Set<MemoryLocation> getModifiedMemoryLocations(CFAEdge edge) {
    Set<MemoryLocation> modifiedByEdge = new HashSet<>();
    switch (edge.getEdgeType()) {
      case StatementEdge: {
        AStatementEdge statementEdge = (AStatementEdge) edge;
        AStatement statement = statementEdge.getStatement();
        if (statement instanceof AExpressionAssignmentStatement) {
          modifiedByEdge.addAll(
              edgeAnalyzer
                  .getInvolvedVariableTypes(
                      ((AExpressionAssignmentStatement) statement).getLeftHandSide(), edge)
                  .keySet());
        } else if (statement instanceof AExpressionStatement) {
          return modifiedByEdge;
        } else if (statement instanceof AFunctionCallAssignmentStatement) {
          modifiedByEdge.addAll(
              edgeAnalyzer
                  .getInvolvedVariableTypes(
                      ((AFunctionCallAssignmentStatement) statement).getLeftHandSide(), edge)
                  .keySet());
        }
        return modifiedByEdge;
      }
      // TODO: Find modified variables for other edge types as well
      case ReturnStatementEdge:
      case FunctionCallEdge:
      case AssumeEdge:
      case FunctionReturnEdge:
      case DeclarationEdge:
      case BlankEdge:
      case CallToReturnEdge:
        return modifiedByEdge;
      default:
        throw new AssertionError("Unknown edge type: " + edge.getEdgeType());
    }
  }
}
