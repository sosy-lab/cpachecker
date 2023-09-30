// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.rangedExecInputSequences;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class SequenceGenUtils {
  private static final CharSequence DELIMITER = System.lineSeparator();
  public static final String KEYWORD_FOR_BRANCH_OR_LOOP = "if |if[(]|for |for[(]|while |while[(]";
  private final LogManager logger;
  private final ImmutableSet<CFANode> loopHeads;

  private final ImmutableSet<CFANode> endlessLoopHeads;
  private final Set<Integer> linesWithIfOrLoop;

  public record PathElement(Boolean decision, CFAEdge edge, ARGState prevARGState) {
  }

  public SequenceGenUtils(LogManager pLogger, CFA pCfa) throws CPAException {
    logger = pLogger;
    loopHeads = pCfa.getAllLoopHeads().orElse(ImmutableSet.of());
    if (pCfa.getLoopStructure().isEmpty()) {
      throw new CPAException("Expecting a loopStruct");
    }
    endlessLoopHeads = loopHeads.stream().filter(l -> l.getNumLeavingEdges() == 1 && l.getLeavingEdge(0) instanceof BlankEdge blank && blank.getRawStatement().equals("1"))
        .collect(ImmutableSet.toImmutableSet());
//    endlessLoopHeads = CFAUtils.getEndlessLoopHeads(pCfa.getLoopStructure().orElseThrow()).stream().collect(ImmutableSet.toImmutableSet());
    linesWithIfOrLoop = new HashSet<>();
    try (Stream<String> linesStream = Files.lines(pCfa.getFileNames().get(0),
        StandardCharsets.UTF_8)) {
      List<String> lines = linesStream.collect(ImmutableList.toImmutableList());
      for (int i = 0; i < lines.size(); i++) {
        Pattern pattern = Pattern.compile(KEYWORD_FOR_BRANCH_OR_LOOP);
        if (pattern.matcher(lines.get(i)).find()) {
          linesWithIfOrLoop.add(i + 1);
        }
      }
    } catch (IOException pE) {
      throw new CPAException(String.format(
          "Failed to read the original program file due to '%s'. Hence, we cannot determine the lines with branches and loops, thus aborting!",
          pE));
    }
    logger.log(Level.INFO, String.format("Lines with branch or loop are %s",
        linesWithIfOrLoop.stream().sorted().collect(
            ImmutableList.toImmutableList())));
    logger.log(Level.INFO, String.format("Loopheads are %s\n, endless loopheads are %s",
        loopHeads.stream().map(l -> l.getEnteringEdge(0).getLineNumber())
            .collect(ImmutableSet.toImmutableSet()),
        endlessLoopHeads.stream().map(l -> l.getEnteringEdge(0).getLineNumber())
            .collect(ImmutableSet.toImmutableSet())));

  }


  public List<Pair<Boolean, Integer>> computeSequenceForLoopbound(
      ARGPath pARGPath,
      Set<String> blacklist, Optional<ARGState> lastState) throws CPAException {
    logger.log(Level.INFO, pARGPath);

    // Check, if the given path is sat by conjoining the path formulae of the abstraction locations.
    // If not, cut off the last part and recursively continue.
    List<PathElement> decisionNodesTaken = new ArrayList<>();
    PathIterator pathIterator = pARGPath.fullPathIterator();
    do {
      if (pathIterator.isPositionWithState()) {
        final ARGState abstractState = pathIterator.getAbstractState();
        if (lastState.isPresent() && abstractState.equals(lastState.orElseThrow())) {
          break;
        }
        if (pathIterator.hasNext()) {
          @Nullable CFAEdge edge = pathIterator.getOutgoingEdge();
          @Nullable CallstackState callState =
              AbstractStates.extractStateByType(abstractState, CallstackState.class);
          if (edge != null && callState != null) {
            // Check  if the edge is an assignment with random function at rhs
            if (blacklist.contains(callState.getCurrentFunction())) {
              logger.log(Level.FINE,
                  String.format("Ignoring edge %s as it is part of the blacklist", edge));
              continue;
            }
            logger.log(Level.FINE, callState.getCurrentFunction());
            if (edge instanceof CAssumeEdge assumeEdge) {
              boolean decision = (assumeEdge.getTruthAssumption() && !assumeEdge.isSwapped()) || (
                  !assumeEdge.getTruthAssumption() && assumeEdge.isSwapped());
              addIfNewDecision(decisionNodesTaken, assumeEdge, abstractState, decision, pARGPath);
            } else if (endlessLoopHeads.contains(edge.getPredecessor())) {
              addIfNewDecision(decisionNodesTaken, edge, abstractState, true, pARGPath);
            }
          }
        }
      }
    } while (pathIterator.advanceIfPossible());

    checkForEndlessLoopheads(decisionNodesTaken, pARGPath);

    ImmutableList<Pair<Boolean, Integer>> resultList;
    resultList = decisionNodesTaken.stream()
        .map(e -> Pair.of(e.decision, e.edge.getLineNumber())).collect(
            ImmutableList.toImmutableList());
    logger.log(Level.INFO, resultList);
    return resultList;
  }

  private void checkForEndlessLoopheads(List<PathElement> pDecisionNodesTaken, ARGPath pARGPath) {
    @Nullable CFANode lastLoc = AbstractStates.extractLocation(pARGPath.getLastState());
    if (lastLoc != null && endlessLoopHeads.contains(lastLoc)) {
      pDecisionNodesTaken.add(
          new PathElement(false, lastLoc.getEnteringEdge(0), pARGPath.getLastState()));
    }
  }

  private void addIfNewDecision(
      List<PathElement> decisionNodesTaken,
      CFAEdge edge,
      ARGState pAbstractState,
      boolean decision, ARGPath pARGPath) throws CPAException {


    Optional<PathElement> lastEntry = Optional.ofNullable(
        decisionNodesTaken.isEmpty() ? null
                                     : decisionNodesTaken.get(decisionNodesTaken.size() - 1));
    // Check if the current entry is a loophead, then add it anyway
    if (loopHeads.contains(edge.getSuccessor())) {
      decisionNodesTaken.add(new PathElement(decision, edge, pAbstractState));

    } else if (linesWithIfOrLoop.contains(edge.getLineNumber())) {
      if (lastEntry.isPresent()) {
        PathElement entry = lastEntry.orElseThrow();
        if (entry.edge.getLineNumber() == edge.getLineNumber() &&
            entry.edge.getRawStatement().equals(edge.getRawStatement())
            // we need to compare the raw statements, as we might have multiple assume-edges for the same line number but for different parts of the condition (happens if we have a && or ||)
            && sameCallingContext(entry.prevARGState, pAbstractState)
            && noLoopHeadInBetween(
            entry.prevARGState, pAbstractState, pARGPath)) {
          logger.log(Level.INFO, String.format(
              "Overwriting decision for edge %s (that was %s), with %s and decision '%s' ",
              entry.edge, entry.decision, edge, decision));
          decisionNodesTaken.remove(entry);
        }
      }
      decisionNodesTaken.add(new PathElement(decision, edge, pAbstractState));
    } else {
      logger.log(Level.INFO, String.format("Ignoring %s with value %s", edge, decision));
    }
  }

  private boolean sameCallingContext(ARGState pFirstState, ARGState pSecondState) {
    @Nullable CallstackState firstCallingContext =
        AbstractStates.extractStateByType(pFirstState, CallstackState.class);
    @Nullable CallstackState secondCallingContext =
        AbstractStates.extractStateByType(pSecondState, CallstackState.class);

    return firstCallingContext != null && firstCallingContext.equals(secondCallingContext);

  }

  private boolean noLoopHeadInBetween(
      ARGState pFirstState,
      ARGState pSecondState,
      ARGPath pARGPath) throws CPAException {
    boolean firstFound = false;

    PathIterator pathIterator = pARGPath.fullPathIterator();
    do {
      if (pathIterator.isPositionWithState()) {
        final ARGState abstractState = pathIterator.getAbstractState();

        if (abstractState.equals(pFirstState)) {
          firstFound = true;
        } else if (firstFound && loopHeads.contains(
            AbstractStates.extractLocation(abstractState))) {
          return false;
        } else if (firstFound && abstractState.equals(pSecondState)) {
          return true;
        }
      } else {
        @Nullable CFAEdge outEdge = pathIterator.getOutgoingEdge();
        if (outEdge != null && loopHeads.contains(outEdge.getPredecessor())) {
          return true;
        }
      }

    } while (pathIterator.advanceIfPossible());
    throw new CPAException("The second node is not on the path!");
  }

  public void printFileToOutput(List<Pair<Boolean, Integer>> pInputs, Path testcaseName)
      throws IOException {

    logger.logf(Level.INFO, "Storing the testcase at %s", testcaseName.toAbsolutePath().toString());
    List<String> content = new ArrayList<>();

    content.add(
        String.join(
            DELIMITER,
            pInputs.stream()
                .map(pair -> {
                  assert pair.getSecond() != null;
                  assert pair.getFirst() != null;
                  return pair.getSecond().toString() + "," + pair.getFirst().toString();
                })
                .collect(ImmutableList.toImmutableList())));
    IO.writeFile(testcaseName, Charset.defaultCharset(), Joiner.on("\n").join(content));
  }
}
