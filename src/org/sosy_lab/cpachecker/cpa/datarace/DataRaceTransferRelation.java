// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAEdgeVisitor;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DataRaceTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;

  public DataRaceTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    return ImmutableList.of(pState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {

    ThreadingState threadingState;
    try {
      threadingState =
          AbstractStates.projectToType(otherStates, ThreadingState.class).first().get();
    } catch (RuntimeException e) {
      throw new CPATransferException("Expected to discover exactly one ThreadingState!",e);
    }

    List<Collection<CFAEdge>> bundles = new ArrayList<>();
    for (CFANode node : threadingState.getLocationNodes()) {
      ImmutableList.Builder<CFAEdge> l = ImmutableList.builder();
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        l.add(node.getLeavingEdge(i));
      }
      bundles.add(l.build());
    }
    SetMultimap<MemoryLocation, Integer> writtenBy = HashMultimap.create();
    SetMultimap<MemoryLocation, Integer> readBy = HashMultimap.create();
    if (bundles.size() <= 1) {
      return ImmutableList.of(state);
    }

    for (int i = 0; i < bundles.size(); i++) {
      Collection<CFAEdge> bundle = bundles.get(i);
      // Set<MemoryLocation> locs = new HashSet<>();
      for (CFAEdge edge : bundle) {
        Collection<MemoryLocation> w = new WriteLocationExtractor().visit(edge);
        Collection<MemoryLocation> r = new ReadLocationExtractor().visit(edge);
        // locs.addAll(extractWriteLocations(edge));
        for (MemoryLocation m : w) {
          writtenBy.put(m, i);
        }
        for (MemoryLocation m : r) {
          readBy.put(m,i);
        }
      }
    }

    boolean hasRace = false;
    for (MemoryLocation m : writtenBy.keySet()) {
      Set<Integer> writeThreads = writtenBy.get(m);
      assert writeThreads.size() >= 1;
      Set<Integer> readThreads = readBy.get(m);
      if (writeThreads.size() > 1
          || (readThreads.size() == 1
              && writeThreads.size() == 1
              && !writeThreads.containsAll(readThreads))
          || readThreads.size() > 1) {
        hasRace = true;
        final String reading =
            readThreads.size() == 0 ? "" : String.format("and threads %s reading ", readThreads);
        logger.log(
            Level.INFO,
            String.format(
                "Data race found with threads %s writing %sthe memory location %s.",
                writeThreads, reading, m));
      }
    }
    if (hasRace) {
      return ImmutableList.of(new DataRaceState(true, writtenBy));
    }
    return Collections.singleton(state);
  }

  private class WriteLocationExtractor implements CFAEdgeVisitor<Collection<MemoryLocation>> {

    @Override
    public Collection<MemoryLocation> handleStatementEdge(AStatementEdge s, AStatement expression)
        throws CPATransferException {
      ImmutableSet.Builder<MemoryLocation> b = ImmutableSet.builder();
      if (expression instanceof AAssignment) {
        AAssignment assignment = (AAssignment) expression;
        AExpression op1 = assignment.getLeftHandSide();
        // ARightHandSide op2 = assignment.getRightHandSide();
        if (op1 instanceof AIdExpression) {
          MemoryLocation memloc =
              ValueAnalysisTransferRelation.getMemoryLocation(
                  (AIdExpression) op1, s.getPredecessor().getFunctionName());
          b.add(memloc);
        }
      }
      return b.build();
    }

    @Override
    public Collection<MemoryLocation> defaultValue() {
      return ImmutableList.of();
    }

  }

  private class ReadLocationExtractor implements CFAEdgeVisitor<Collection<MemoryLocation>> {

    @Override
    public Collection<MemoryLocation> handleStatementEdge(AStatementEdge s, AStatement expression)
        throws CPATransferException {
      if (expression instanceof AAssignment) {
        AAssignment assignment = (AAssignment) expression;
        ARightHandSide rhs = assignment.getRightHandSide();
        return getMemoryLocations(rhs,s.getPredecessor().getFunctionName());
      } else if (expression instanceof AFunctionCallStatement) {
        return ImmutableList.of();
      }
      throw new UnsupportedOperationException(
          String.format("Unsupported statement type %s!", expression.getClass()));
    }

    private Collection<MemoryLocation> getMemoryLocations(AAstNode node, String functionName) {
      return CFAUtils.traverseRecursively(node)
          .filter(AIdExpression.class)
          .stream()
          .map(x -> ValueAnalysisTransferRelation.getMemoryLocation((x), functionName))
          .collect(ImmutableList.toImmutableList());
    }
    /*.*/

    @Override
    public Collection<MemoryLocation> defaultValue() {
      return ImmutableList.of();
    }
  }
}


