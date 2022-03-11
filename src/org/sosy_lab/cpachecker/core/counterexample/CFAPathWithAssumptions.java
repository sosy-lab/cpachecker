// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.IntermediateConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.SingleConcreteState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;

/**
 * This class represents a path of cfaEdges, that contain the additional Information at which edge
 * which assignableTerm was created when this path was checked by the class {@link PathChecker}.
 */
public class CFAPathWithAssumptions extends ForwardingList<CFAEdgeWithAssumptions> {

  private final ImmutableList<CFAEdgeWithAssumptions> pathWithAssignments;

  private CFAPathWithAssumptions(ImmutableList<CFAEdgeWithAssumptions> pPathWithAssignments) {
    pathWithAssignments = ImmutableList.copyOf(pPathWithAssignments);
  }

  public static CFAPathWithAssumptions empty() {
    return new CFAPathWithAssumptions(ImmutableList.of());
  }

  @Override
  protected List<CFAEdgeWithAssumptions> delegate() {
    return pathWithAssignments;
  }

  boolean fitsPath(List<CFAEdge> pPath) {
    int index = 0;

    for (CFAEdge edge : pPath) {
      CFAEdgeWithAssumptions cfaWithAssignment = pathWithAssignments.get(index);

      if (!edge.equals(cfaWithAssignment.getCFAEdge())) {
        return false;
      }
      index++;
    }

    return true;
  }

  public ImmutableSetMultimap<ARGState, CFAEdgeWithAssumptions> getExactVariableValues(
      ARGPath pPath) {
    ImmutableSetMultimap.Builder<ARGState, CFAEdgeWithAssumptions> result =
        ImmutableSetMultimap.builder();

    PathIterator pathIterator = pPath.fullPathIterator();
    int multiEdgeOffset = 0;

    while (pathIterator.hasNext()) {
      CFAEdgeWithAssumptions edgeWithAssignment =
          pathWithAssignments.get(pathIterator.getIndex() + multiEdgeOffset);
      CFAEdge argPathEdge = pathIterator.getOutgoingEdge();

      if (!edgeWithAssignment.getCFAEdge().equals(argPathEdge)) {
        // path is not equivalent
        return ImmutableSetMultimap.of();
      }

      final ARGState abstractState;
      if (pathIterator.isPositionWithState()) {
        abstractState = pathIterator.getAbstractState();
      } else {
        abstractState = pathIterator.getPreviousAbstractState();
      }
      result.put(abstractState, edgeWithAssignment);

      pathIterator.advance();
    }
    // last state is ignored

    return result.build();
  }

  public static CFAPathWithAssumptions of(
      ConcreteStatePath statePath, AssumptionToEdgeAllocator pAllocator) {

    ImmutableList.Builder<CFAEdgeWithAssumptions> result =
        ImmutableList.builderWithExpectedSize(statePath.size());
    List<IntermediateConcreteState> currentIntermediateStates = new ArrayList<>();

    for (ConcreteStatePathNode node : statePath) {
      CFAEdgeWithAssumptions edge;

      // this is an intermediate state: just create the assumptions for it
      // and add it as if it was a normal edge
      if (node instanceof IntermediateConcreteState) {
        IntermediateConcreteState intermediateState = (IntermediateConcreteState) node;
        currentIntermediateStates.add(intermediateState);
        edge =
            pAllocator.allocateAssumptionsToEdge(
                intermediateState.getCfaEdge(), intermediateState.getConcreteState());

      } else {
        SingleConcreteState singleState = (SingleConcreteState) node;

        // no ARG hole, just a normal edge
        if (currentIntermediateStates.isEmpty()) {
          edge =
              pAllocator.allocateAssumptionsToEdge(
                  singleState.getCfaEdge(), singleState.getConcreteState());

          /* End of an ARG hole, handle all the intermediate edges before
           * and create the assumptions at the end of the (dynamic) multi edge
           * for all changed variables.Since it is impossible to properly project
           * the assumption from the assumptions of the edges in the multi edge,
           * due to aliasing, simply create assumptions for all edges with the concrete state
           * of the last edge, thus correctly projecting all lvalues at the end of the multi edge.*/
        } else {
          ImmutableSet.Builder<AExpressionStatement> assumptions = ImmutableSet.builder();
          Set<String> assumptionCodes = new HashSet<>();
          ConcreteState lastState = singleState.getConcreteState();

          StringBuilder comment = new StringBuilder("");

          if (!isEmptyDeclaration(singleState.getCfaEdge())) {
            for (IntermediateConcreteState intermediates : currentIntermediateStates) {
              CFAEdgeWithAssumptions assumptionForedge =
                  pAllocator.allocateAssumptionsToEdge(intermediates.getCfaEdge(), lastState);
              addAssumptionsIfNecessary(assumptions, assumptionCodes, comment, assumptionForedge);
            }

            // add assumptions for last edge if necessary
            addAssumptionsIfNecessary(
                assumptions,
                assumptionCodes,
                comment,
                pAllocator.allocateAssumptionsToEdge(singleState.getCfaEdge(), lastState));
          }

          // Finally create Last edge and multi edge
          edge =
              new CFAEdgeWithAssumptions(
                  singleState.getCfaEdge(), assumptions.build(), comment.toString());

          // remove all handled intermediate states
          currentIntermediateStates.clear();
        }
      }

      // add created edge to result
      result.add(edge);
    }

    return new CFAPathWithAssumptions(result.build());
  }

  private static boolean isEmptyDeclaration(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof ADeclarationEdge) {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pCfaEdge;
      ADeclaration declaration = declarationEdge.getDeclaration();
      if (declaration instanceof AVariableDeclaration) {
        AVariableDeclaration variableDeclaration = (AVariableDeclaration) declaration;
        return variableDeclaration.getInitializer() == null && !variableDeclaration.isGlobal();
      }
      return true;
    }
    return false;
  }

  private static void addAssumptionsIfNecessary(
      ImmutableCollection.Builder<AExpressionStatement> assumptions,
      Set<String> assumptionCodes,
      StringBuilder comment,
      CFAEdgeWithAssumptions lastIntermediate) {
    // throw away redundant assumptions
    for (AExpressionStatement assumption : lastIntermediate.getExpStmts()) {
      if (!assumptionCodes.contains(assumption.toASTString())) {
        assumptions.add(assumption);
        assumptionCodes.add(assumption.toASTString());
      }
    }

    String commentOfEdge = lastIntermediate.getComment();

    if (!isNullOrEmpty(commentOfEdge)) {
      comment.append(commentOfEdge);
      comment.append("\n");
    }
  }

  public Optional<CFAPathWithAssumptions> mergePaths(CFAPathWithAssumptions pOtherPath) {

    if (pOtherPath.size() != size()) {
      return Optional.empty();
    }

    ImmutableList.Builder<CFAEdgeWithAssumptions> result =
        ImmutableList.builderWithExpectedSize(size());
    Iterator<CFAEdgeWithAssumptions> path2Iterator = iterator();

    for (CFAEdgeWithAssumptions edge : this) {
      CFAEdgeWithAssumptions other = path2Iterator.next();
      if (!edge.getCFAEdge().equals(other.getCFAEdge())) {
        return Optional.empty();
      }
      CFAEdgeWithAssumptions resultEdge = edge.mergeEdge(other);
      result.add(resultEdge);
    }

    return Optional.of(new CFAPathWithAssumptions(result.build()));
  }

  public static CFAPathWithAssumptions of(
      ARGPath pPath,
      ConfigurableProgramAnalysis pCPA,
      AssumptionToEdgeAllocator pAssumptionToEdgeAllocator) {

    FluentIterable<ConfigurableProgramAnalysisWithConcreteCex> cpas =
        CPAs.asIterable(pCPA).filter(ConfigurableProgramAnalysisWithConcreteCex.class);

    Optional<CFAPathWithAssumptions> result = Optional.empty();

    for (ConfigurableProgramAnalysisWithConcreteCex wrappedCpa : cpas) {
      ConcreteStatePath path = wrappedCpa.createConcreteStatePath(pPath);
      CFAPathWithAssumptions cexPath = CFAPathWithAssumptions.of(path, pAssumptionToEdgeAllocator);

      if (result.isPresent()) {
        result = result.orElseThrow().mergePaths(cexPath);
        // If there were conflicts during merging, stop
        if (!result.isPresent()) {
          break;
        }
      } else {
        result = Optional.of(cexPath);
      }
    }

    if (!result.isPresent()) {
      return CFAPathWithAssumptions.empty();
    } else {
      return result.orElseThrow();
    }
  }
}
