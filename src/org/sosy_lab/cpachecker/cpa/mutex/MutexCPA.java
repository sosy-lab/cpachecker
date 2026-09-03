// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

/**
 * CPA that tracks the state of mutexes in concurrent C programs, supporting both POSIX pthread
 * mutexes and C11 threading mutexes.
 *
 * <p>This CPA maintains a {@link MutexState} that records which mutexes have been initialized and
 * which are currently locked (and by which thread). It communicates with the POR CPA via the {@code
 * strengthen} operator to learn the executing thread's PID.
 */
public class MutexCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(MutexCPA.class);
  }

  public MutexCPA(CFA pCFA) throws InvalidConfigurationException {
    super("sep", "sep", new MutexTransferRelation(collectMutexHandleCandidates(pCFA)));
  }

  private static ImmutableSet<String> collectMutexHandleCandidates(CFA pCFA) {
    Set<String> variableDeclarations = new HashSet<>();
    Map<String, Integer> assignmentCounts = new HashMap<>();

    for (CFAEdge edge : pCFA.edges()) {
      if (edge instanceof CDeclarationEdge declarationEdge) {
        CDeclaration declaration = declarationEdge.getDeclaration();
        // instanceof CVariableDeclaration already excludes CParameterDeclaration (a distinct
        // AST type), but the intent is spelled out explicitly since that was a requirement.
        if (declaration instanceof CVariableDeclaration variableDeclaration) {
          String name = variableDeclaration.getQualifiedName();
          variableDeclarations.add(name);
          if (variableDeclaration.getInitializer() != null) {
            assignmentCounts.merge(name, 1, Integer::sum);
          } else {
            assignmentCounts.merge(name, 0, Integer::sum);
          }
        }
      } else if (edge instanceof CStatementEdge statementEdge) {
        CStatement statement = statementEdge.getStatement();
        if (statement instanceof CAssignment assignment) {
          CLeftHandSide lhs = assignment.getLeftHandSide();
          if (lhs instanceof CIdExpression idExpression
              && idExpression.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
            assignmentCounts.merge(variableDeclaration.getQualifiedName(), 1, Integer::sum);
          }
        }
      }
    }

    ImmutableSet.Builder<String> candidates = ImmutableSet.builder();
    for (String name : variableDeclarations) {
      int count = assignmentCounts.getOrDefault(name, -1);
      if (0 <= count && count <= 1) {
        candidates.add(name);
      }
    }
    return candidates.build();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return MutexState.EMPTY;
  }
}
