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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Equal;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalAnd;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalNot;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

enum AbstractionStateFactories implements AbstractionStateFactory {

  ALWAYS {

    @Override
    public AbstractionState getSuccessorState(AbstractionState pPrevious) {
      return BasicAbstractionStates.ALWAYS_STATE;
    }

    @Override
    public AbstractionState getAbstractionState() {
      return getSuccessorState(null);
    }

    @Override
    public AbstractionState from(AbstractionState pOther) {
      return getAbstractionState();
    }

  },

  ENTERING_EDGES {

    @Override
    public AbstractionState getAbstractionState() {
      return getSuccessorState(null);
    }

    @Override
    public AbstractionState from(AbstractionState pOther) {
      return from(pOther, true);
    }

    @Override
    public AbstractionState getSuccessorState(AbstractionState pOther) {
      return from(pOther, false);
    }

    private AbstractionState from(final AbstractionState pPrevious, boolean pWithEnteringEdges) {
      class EnteringEdgesBasedAbstractionState implements AbstractionState {

        private final Set<CFAEdge> visitedEdges;

        private final Set<String> wideningTargets;

        private final Set<InvariantsFormula<CompoundInterval>> wideningHints;

        private EnteringEdgesBasedAbstractionState(
            Set<String> pPreviousWideningTargets,
            Set<InvariantsFormula<CompoundInterval>> pPreviousWideningHints) {
          this(Collections.<CFAEdge>emptySet(), pPreviousWideningTargets, pPreviousWideningHints);
        }

        private EnteringEdgesBasedAbstractionState(
            Set<CFAEdge> pVisitedEdges,
            Set<String> pWideningTargets,
            Set<InvariantsFormula<CompoundInterval>> pWideningHints) {
          this.visitedEdges = pVisitedEdges;
          this.wideningTargets = pWideningTargets;
          this.wideningHints = pWideningHints;
        }

        private ImmutableSet<String> determineWideningTargets(CFAEdge pEdge) {
          return determineWideningTargets(Collections.singleton(pEdge));
        }

        private ImmutableSet<String> determineWideningTargets(Iterable<CFAEdge> pEdges) {
          ImmutableSet.Builder<String> wideningTargets = ImmutableSet.builder();
          Set<CFAEdge> checkedEdges = new HashSet<>();
          Queue<CFAEdge> waitlist = new ArrayDeque<>();
          Iterables.addAll(waitlist, pEdges);

          while (!waitlist.isEmpty()) {
            CFAEdge lastEdge = waitlist.poll();
            checkedEdges.add(lastEdge);
            if (lastEdge.getEdgeType() == CFAEdgeType.MultiEdge) {
              Iterables.addAll(waitlist, (MultiEdge) lastEdge);
              continue;
            }
            if (lastEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
              FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) lastEdge;

              Set<CFANode> visited = new HashSet<>();
              Queue<CFANode> successors = new ArrayDeque<>();
              successors.offer(functionReturnEdge.getPredecessor());

              while (!successors.isEmpty()) {
                CFANode current = successors.poll();
                for (CFAEdge enteringEdge : CFAUtils.allEnteringEdges(current)) {
                  if (enteringEdge.getEdgeType() != CFAEdgeType.FunctionCallEdge) {
                    CFANode newSucc = enteringEdge.getPredecessor();
                    if (visited.add(newSucc)) {
                      if (enteringEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
                        successors.add(((FunctionReturnEdge) enteringEdge).getSummaryEdge().getPredecessor());
                      } else {
                        successors.offer(newSucc);
                      }
                      if (!checkedEdges.contains(enteringEdge)) {
                        waitlist.add(enteringEdge);
                      }
                    }
                  }
                }
              }

              FunctionSummaryEdge summaryEdge = functionReturnEdge.getSummaryEdge();
              if (summaryEdge != null) {
                AFunctionCall functionCall = summaryEdge.getExpression();
                if (functionCall instanceof AFunctionCallAssignmentStatement) {
                  AFunctionCallAssignmentStatement assignmentStatement = (AFunctionCallAssignmentStatement) functionCall;
                  wideningTargets.addAll(EdgeAnalyzer.getInvolvedVariables(assignmentStatement.getLeftHandSide(), summaryEdge).keySet());
                  continue;
                }
              }
            }
            if (lastEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
              AStatementEdge edge = (AStatementEdge) lastEdge;
              if (edge.getStatement() instanceof AExpressionStatement) {
                AExpressionStatement expressionStatement = (AExpressionStatement) edge.getStatement();
                AExpression expression = expressionStatement.getExpression();
                if (expression instanceof ALiteralExpression) {
                  continue;
                }
                if (expression instanceof ALeftHandSide) {
                  continue;
                }
              } else if (edge.getStatement() instanceof AExpressionAssignmentStatement) {
                AExpressionAssignmentStatement expressionAssignmentStatement = (AExpressionAssignmentStatement) edge.getStatement();
                AExpression expression = expressionAssignmentStatement.getRightHandSide();
                if (expression instanceof ALiteralExpression) {
                  continue;
                }
                if (expression instanceof ALeftHandSide) {
                  continue;
                }
              }
            }
            if (lastEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
              continue;
            }
            if (lastEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
              ADeclarationEdge edge = (ADeclarationEdge) lastEdge;
              ADeclaration declaration = edge.getDeclaration();
              if (declaration instanceof AVariableDeclaration) {
                AVariableDeclaration variableDeclaration = (AVariableDeclaration) declaration;
                AInitializer initializer = variableDeclaration.getInitializer();
                if (initializer == null) {
                  continue;
                }
                if (initializer instanceof AInitializerExpression) {
                  AExpression expression = ((AInitializerExpression) initializer).getExpression();
                  if (expression instanceof ALiteralExpression) {
                    continue;
                  }
                  if (expression instanceof ALeftHandSide) {
                    continue;
                  }
                }
              }
            }
            wideningTargets.addAll(EdgeAnalyzer.getInvolvedVariables(lastEdge).keySet());
          }
          return wideningTargets.build();
        }

        @Override
        public Set<String> determineWideningTargets(AbstractionState pOther) {
          if (pOther instanceof EnteringEdgesBasedAbstractionState) {
            EnteringEdgesBasedAbstractionState other = (EnteringEdgesBasedAbstractionState) pOther;
            if (!visitedEdges.containsAll(other.visitedEdges)) {
              return Collections.emptySet();
            }
            return union(wideningTargets, other.wideningTargets);
          }
          return wideningTargets;
        }

        @Override
        public AbstractionState addEnteringEdge(CFAEdge pEdge) {
          Set<String> newWideningTargets = determineWideningTargets(pEdge);
          Set<InvariantsFormula<CompoundInterval>> newWideningHints = determineWideningHints(pEdge);
          if (visitedEdges.contains(pEdge)
              && wideningTargets.containsAll(newWideningTargets)
              && wideningHints.containsAll(newWideningHints)) {
            return this;
          }
          newWideningHints = union(wideningHints, newWideningHints);
          newWideningTargets = union(wideningTargets, newWideningTargets);
          return new EnteringEdgesBasedAbstractionState(
              add(visitedEdges, pEdge),
              newWideningTargets, newWideningHints);
        }

        private Set<InvariantsFormula<CompoundInterval>> determineWideningHints(CFAEdge pEdge) {
          if (pEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
            AssumeEdge assumeEdge = (AssumeEdge) pEdge;
            AExpression expression = assumeEdge.getExpression();
            final InvariantsFormula<CompoundInterval> wideningHint;
            try {
              ExpressionToFormulaVisitor expressionToFormulaVisitor =
                  new ExpressionToFormulaVisitor(
                      new VariableNameExtractor(
                          pEdge,
                          Collections.<String, InvariantsFormula<CompoundInterval>>emptyMap()));
              if (expression instanceof CExpression) {
                wideningHint = ((CExpression) expression).accept(expressionToFormulaVisitor);
              } else if (expression instanceof JExpression) {
                wideningHint = ((JExpression) expression).accept(expressionToFormulaVisitor);
              } else {
                return Collections.emptySet();
              }
            } catch (UnrecognizedCodeException e) {
              // Does not really matter, just no hint
              return Collections.emptySet();
            }
            return normalize(Collections.singleton(wideningHint));
          }
          return Collections.emptySet();
        }

        private ImmutableSet<InvariantsFormula<CompoundInterval>> normalize(
            Set<InvariantsFormula<CompoundInterval>> pToNormalize) {
          ImmutableSet.Builder<InvariantsFormula<CompoundInterval>> builder = ImmutableSet.builder();
          Queue<InvariantsFormula<CompoundInterval>> toNormalize = new ArrayDeque<>(pToNormalize);
          while (!toNormalize.isEmpty()) {
            InvariantsFormula<CompoundInterval> hint = toNormalize.poll();
            if (!CompoundIntervalFormulaManager.collectVariableNames(hint).isEmpty()) {
              if (hint instanceof LogicalNot) {
                toNormalize.offer(((LogicalNot<CompoundInterval>) hint).getNegated());
              } else if (hint instanceof LogicalAnd) {
                toNormalize.offer(((LogicalAnd<CompoundInterval>) hint).getOperand1());
                toNormalize.offer(((LogicalAnd<CompoundInterval>) hint).getOperand2());
              } else {
                builder.add(hint);
                builder.add(CompoundIntervalFormulaManager.INSTANCE.negate(hint));
                if (hint instanceof Equal) {
                  Equal<CompoundInterval> eq = (Equal<CompoundInterval>) hint;
                  toNormalize.offer(CompoundIntervalFormulaManager.INSTANCE.lessThan(eq.getOperand1(), eq.getOperand2()));
                  toNormalize.offer(CompoundIntervalFormulaManager.INSTANCE.greaterThan(eq.getOperand1(), eq.getOperand2()));
                }
              }
            }
          }
          return builder.build();
        }

        @Override
        public AbstractionState join(AbstractionState pOther) {
          if (pOther == BasicAbstractionStates.NEVER_STATE || pOther == this) {
            return this;
          }
          if (pOther instanceof EnteringEdgesBasedAbstractionState) {
            EnteringEdgesBasedAbstractionState other = (EnteringEdgesBasedAbstractionState) pOther;
            if ((this.visitedEdges == other.visitedEdges || other.visitedEdges.containsAll(this.visitedEdges))
                && (this.wideningTargets == other.wideningTargets || other.wideningTargets.containsAll(this.wideningTargets))
                && (this.wideningHints == other.wideningHints || other.wideningHints.containsAll(this.wideningHints))) {
              return other;
            }
            if ((this.visitedEdges.containsAll(other.visitedEdges))
                && this.wideningTargets.containsAll(other.wideningTargets)) {
              return this;
            }
            final Set<CFAEdge> edges =
                union(visitedEdges, other.visitedEdges);
            final Set<String> lastEdges =
                union(wideningTargets, other.wideningTargets);
            final Set<InvariantsFormula<CompoundInterval>> hints =
                union(wideningHints, other.wideningHints);
            return new EnteringEdgesBasedAbstractionState(edges, lastEdges, hints);
          }
          return BasicAbstractionStates.ALWAYS_STATE;
        }

        @Override
        public boolean equals(Object pO) {
          if (this == pO) {
            return true;
          }
          if (pO instanceof EnteringEdgesBasedAbstractionState) {
            EnteringEdgesBasedAbstractionState other = (EnteringEdgesBasedAbstractionState) pO;
            return wideningTargets.equals(other.wideningTargets)
                && visitedEdges.equals(other.visitedEdges)
                && wideningHints.equals(other.wideningHints);
          }
          return false;
        }

        @Override
        public int hashCode() {
          return Objects.hash(visitedEdges, wideningTargets, wideningHints);
        }

        @Override
        public String toString() {
          return String.format("Widening targets: %s; Visited edges: %s", wideningTargets, visitedEdges.toString());
        }

        @Override
        public boolean isLessThanOrEqualTo(AbstractionState pOther) {
          if (pOther instanceof EnteringEdgesBasedAbstractionState) {
            EnteringEdgesBasedAbstractionState other = (EnteringEdgesBasedAbstractionState) pOther;
            return other.visitedEdges.containsAll(this.visitedEdges);
          }
          return !pOther.isLessThanOrEqualTo(this);
        }

        @Override
        public Set<InvariantsFormula<CompoundInterval>> getWideningHints() {
          return this.wideningHints;
        }

      }
      if (pWithEnteringEdges && pPrevious instanceof EnteringEdgesBasedAbstractionState) {
        return pPrevious;
      }
      final Set<String> previousWideningTargets;
      final Set<InvariantsFormula<CompoundInterval>> previousWideningHints;
      if (pPrevious instanceof EnteringEdgesBasedAbstractionState) {
        previousWideningTargets = ((EnteringEdgesBasedAbstractionState) pPrevious).wideningTargets;
        previousWideningHints = ((EnteringEdgesBasedAbstractionState) pPrevious).wideningHints;
      } else {
        previousWideningTargets = Collections.emptySet();
        previousWideningHints = Collections.emptySet();
      }
      return new EnteringEdgesBasedAbstractionState(previousWideningTargets, previousWideningHints);
    }

  },

  NEVER {

    @Override
    public AbstractionState getSuccessorState(AbstractionState pPrevious) {
      return BasicAbstractionStates.NEVER_STATE;
    }

    @Override
    public AbstractionState getAbstractionState() {
      return getSuccessorState(null);
    }

    @Override
    public AbstractionState from(AbstractionState pOther) {
      return getAbstractionState();
    }

  };

  /**
   * Returns the union of the given sets.
   *
   * If both parameters are immutable sets, the returned set is guaranteed to
   * be immutable.
   *
   * The result may or may not be backed by either of the sets.
   *
   * @param pSet1 the first set.
   * @param pSet2 the second set.
   *
   * @return the union of the given sets.
   */
  private static <T> Set<T> union(Set<T> pSet1, Set<T> pSet2) {
    if (pSet1 == pSet2 || pSet2.containsAll(pSet1)) {
      return pSet2;
    }
    if (pSet1.containsAll(pSet2)) {
      return pSet1;
    }
    return new ImmutableSet.Builder<T>().addAll(pSet1).addAll(pSet2).build();
  }

  /**
   * Returns the union of the given set and the set with the given element.
   *
   * If the given set is immutable, the result is guaranteed to be immutable.
   *
   * This set may or may not be backed by the given set.
   *
   * @param pSet the set.
   * @param pElement the element to add.
   *
   * @return a set containing only the elements contained in the given set and
   * the given element.
   */
  private static <T> Set<T> add(Set<T> pSet, T pElement) {
    return union(pSet, Collections.singleton(pElement));
  }

  private static enum BasicAbstractionStates implements AbstractionState {

    ALWAYS_STATE {

      @Override
      public Set<String> determineWideningTargets(AbstractionState pOther) {
        return null;
      }

      @Override
      public AbstractionState addEnteringEdge(CFAEdge pEdge) {
        return this;
      }

      @Override
      public AbstractionState join(AbstractionState pOther) {
        return this;
      }

      @Override
      public boolean isLessThanOrEqualTo(AbstractionState pOther) {
        return equals(pOther);
      }

      @Override
      public Set<InvariantsFormula<CompoundInterval>> getWideningHints() {
        return Collections.emptySet();
      }

    },

    NEVER_STATE {

      @Override
      public Set<String> determineWideningTargets(AbstractionState pOther) {
        return Collections.emptySet();
      }

      @Override
      public AbstractionState addEnteringEdge(CFAEdge pEdge) {
        return this;
      }

      @Override
      public AbstractionState join(AbstractionState pOther) {
        if (pOther == this) {
          return this;
        }
        return pOther.join(this);
      }

      @Override
      public boolean isLessThanOrEqualTo(AbstractionState pOther) {
        return true;
      }

      @Override
      public Set<InvariantsFormula<CompoundInterval>> getWideningHints() {
        return Collections.emptySet();
      }

    }

  }

}