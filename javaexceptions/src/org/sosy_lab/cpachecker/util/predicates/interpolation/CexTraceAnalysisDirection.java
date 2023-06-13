// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import com.google.common.primitives.ImmutableIntArray;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;

enum CexTraceAnalysisDirection {
  /** Just the trace as it is */
  FORWARDS {
    @Override
    public ImmutableIntArray orderFormulas(
        List<BooleanFormula> traceFormulas,
        List<AbstractState> abstractionStates,
        VariableClassification pVariableClassification,
        LoopStructure pLoopStructure,
        FormulaManagerView pFmgr) {
      return ImmutableIntArray.copyOf(IntStream.range(0, traceFormulas.size()));
    }
  },

  /** The trace when traversed backwards */
  BACKWARDS {
    @Override
    public ImmutableIntArray orderFormulas(
        List<BooleanFormula> traceFormulas,
        List<AbstractState> abstractionStates,
        VariableClassification pVariableClassification,
        LoopStructure pLoopStructure,
        FormulaManagerView pFmgr) {
      ImmutableIntArray.Builder order = ImmutableIntArray.builder(traceFormulas.size());
      for (int i = traceFormulas.size() - 1; i >= 0; i--) {
        order.add(i);
      }
      return order.build();
    }
  },

  /** Takes alternatingly one element from the front of the trace and one of the back */
  ZIGZAG {
    @Override
    public ImmutableIntArray orderFormulas(
        List<BooleanFormula> traceFormulas,
        List<AbstractState> abstractionStates,
        VariableClassification pVariableClassification,
        LoopStructure pLoopStructure,
        FormulaManagerView pFmgr) {
      ImmutableIntArray.Builder order = ImmutableIntArray.builder(traceFormulas.size());
      int e = traceFormulas.size() - 1;
      int s = 0;
      boolean fromStart = false;
      while (s <= e) {
        int i = fromStart ? s++ : e--;
        fromStart = !fromStart;

        order.add(i);
      }
      return order.build();
    }
  },

  /**
   * Those parts of the trace that are in no loops or in less loops than others are sorted to the
   * front
   */
  LOOP_FREE_FIRST {
    @Override
    public ImmutableIntArray orderFormulas(
        List<BooleanFormula> traceFormulas,
        List<AbstractState> abstractionStates,
        VariableClassification pVariableClassification,
        LoopStructure pLoopStructure,
        FormulaManagerView pFmgr) {
      ListMultimap<Integer, Integer> loopLevelsToIndexMap =
          MultimapBuilder.treeKeys().arrayListValues().build();
      createLoopDrivenStateOrdering(
          abstractionStates, loopLevelsToIndexMap, new ArrayDeque<CFANode>(), pLoopStructure);

      return ImmutableIntArray.copyOf(loopLevelsToIndexMap.values());
    }
  },

  /** A random order of the trace */
  RANDOM {
    @SuppressWarnings("ImmutableEnumChecker")
    private final Random rnd = new Random(0);

    @Override
    public ImmutableIntArray orderFormulas(
        List<BooleanFormula> traceFormulas,
        List<AbstractState> abstractionStates,
        VariableClassification pVariableClassification,
        LoopStructure pLoopStructure,
        FormulaManagerView pFmgr) {
      int[] array = IntStream.range(0, traceFormulas.size()).toArray();

      // Shuffle array with same strategy as Collections.shuffle:
      // starting with the last element, swap each element with a random one from before it.
      // Elements can be swapped with themselves.
      for (int i = array.length - 1; i > 0; i--) {
        int swapWith = rnd.nextInt(i + 1);
        int temp = array[i];
        array[i] = array[swapWith];
        array[swapWith] = temp;
      }

      return ImmutableIntArray.copyOf(array);
    }
  },

  /**
   * Formulas with the lowest average score for their variables according to some calculations in
   * the VariableClassification are sorted to the front
   */
  LOWEST_AVG_SCORE {
    @Override
    public ImmutableIntArray orderFormulas(
        List<BooleanFormula> traceFormulas,
        List<AbstractState> abstractionStates,
        VariableClassification pVariableClassification,
        LoopStructure pLoopStructure,
        FormulaManagerView pFmgr) {
      // Compute tuple (index, score) for each formula, sort according to score, and return indices
      return ImmutableIntArray.copyOf(
          getIndexScoreTuples(traceFormulas, pVariableClassification, pLoopStructure, pFmgr)
              .sorted(Comparator.comparing(Pair::getSecond))
              .mapToInt(Pair::getFirst));
    }
  },

  /**
   * Formulas with the highest average score for their variables according to some calculations in
   * the VariableClassification are sorted to the front
   */
  HIGHEST_AVG_SCORE {
    @Override
    public ImmutableIntArray orderFormulas(
        List<BooleanFormula> traceFormulas,
        List<AbstractState> abstractionStates,
        VariableClassification pVariableClassification,
        LoopStructure pLoopStructure,
        FormulaManagerView pFmgr) {
      // Compute tuple (index, score) for each formula, sort according to score, and return indices
      final Comparator<Pair<?, Double>> scoreComparator = Comparator.comparing(Pair::getSecond);
      return ImmutableIntArray.copyOf(
          getIndexScoreTuples(traceFormulas, pVariableClassification, pLoopStructure, pFmgr)
              .sorted(scoreComparator.reversed())
              .mapToInt(Pair::getFirst));
    }
  },

  /**
   * Combination of loop free first and backwards, for each loop level, we iterate backwards through
   * the found formulas to have those that are closest to the error location at first.
   */
  LOOP_FREE_FIRST_BACKWARDS {
    @Override
    public ImmutableIntArray orderFormulas(
        List<BooleanFormula> traceFormulas,
        List<AbstractState> abstractionStates,
        VariableClassification pVariableClassification,
        LoopStructure pLoopStructure,
        FormulaManagerView pFmgr) {
      ListMultimap<Integer, Integer> loopLevelsToIndexMap =
          MultimapBuilder.treeKeys().arrayListValues().build();
      createLoopDrivenStateOrdering(
          abstractionStates, loopLevelsToIndexMap, new ArrayDeque<CFANode>(), pLoopStructure);

      ImmutableIntArray.Builder order = ImmutableIntArray.builder(traceFormulas.size());
      for (List<Integer> indices : Multimaps.asMap(loopLevelsToIndexMap).values()) {
        order.addAll(Lists.reverse(indices));
      }
      return order.build();
    }
  };

  /**
   * This method returns a permutation of the interval {@code [0, traceFormulas.size()[} such that
   * when the formulas are reordered according to this permutation, they are in the order as
   * described by the respective enum value.
   */
  public abstract ImmutableIntArray orderFormulas(
      List<BooleanFormula> traceFormulas,
      List<AbstractState> abstractionStates,
      VariableClassification variableClassification,
      LoopStructure loopStructure,
      FormulaManagerView fmgr);

  /** Return a stream with tuples (index, score) for each of the given formulas (in order). */
  private static Stream<Pair<Integer, Double>> getIndexScoreTuples(
      List<BooleanFormula> traceFormulas,
      VariableClassification pVariableClassification,
      LoopStructure pLoopStructure,
      FormulaManagerView pFmgr) {
    return Streams.mapWithIndex(
        traceFormulas.stream(),
        (formula, index) ->
            Pair.of(
                (int) index,
                getAVGScoreForVariables(formula, pVariableClassification, pFmgr, pLoopStructure)));
  }

  /**
   * This method computes a score for a set of variables regarding the domain types of these
   * variables.
   *
   * @return the average score over all given variables
   */
  private static double getAVGScoreForVariables(
      BooleanFormula formula,
      VariableClassification variableClassification,
      FormulaManagerView fmgr,
      LoopStructure loopStructure) {

    Set<String> varNames =
        from(fmgr.extractVariableNames(formula))
            .transform(
                variable -> {
                  Pair<String, OptionalInt> name = FormulaManagerView.parseName(variable);
                  // we want only variables in our set, and ignore everything without SSA index
                  return name.getSecond().isPresent() ? name.getFirst() : null;
                })
            .filter(Predicates.notNull())
            .toSet();

    double currentScore = 0;
    for (String variableName : varNames) {

      // best, easy variables
      if (variableClassification.getIntBoolVars().contains(variableName)) {
        currentScore += 2;

        // little harder but still good variables
      } else if (variableClassification.getIntEqualVars().contains(variableName)) {
        currentScore += 4;

        // unknown type, potentially much harder than other variables
      } else {
        currentScore += 16;
      }

      // a loop counter variables, really bad for interpolants
      if (loopStructure.getLoopIncDecVariables().contains(variableName)) {
        currentScore += 100;
      }

      // check for overflow
      if (currentScore < 0) {
        return Double.MAX_VALUE / varNames.size();
      }
    }

    // this is a true or false formula, return 0 as this is the easiest formula
    // we can encounter
    if (varNames.isEmpty()) {
      return 0;
    } else {
      return currentScore / varNames.size();
    }
  }

  private static void createLoopDrivenStateOrdering(
      final List<AbstractState> pAbstractionStates,
      final Multimap<Integer, Integer> loopLevelsToIndexMap,
      Deque<CFANode> actLevelStack,
      LoopStructure loopStructure) {
    ImmutableSet<CFANode> loopHeads = loopStructure.getAllLoopHeads();

    // in the nodeLoopLevel map there has to be for every seen ARGState one
    // key-value pair therefore we can use this as our index
    int actARGState = loopLevelsToIndexMap.size();

    CFANode actCFANode = null;
    boolean isCFANodeALoopHead = false;

    // move on as long as there occurs no loop-head in the ARG path
    while (!isCFANodeALoopHead
        && actLevelStack.isEmpty()
        && actARGState < pAbstractionStates.size()) {

      actCFANode = AbstractStates.extractLocation(pAbstractionStates.get(actARGState));

      loopLevelsToIndexMap.put(0, actARGState);

      isCFANodeALoopHead = loopHeads.contains(actCFANode);

      actARGState++;
    }

    // when not finished with computing the node levels
    if (actARGState != pAbstractionStates.size()) {
      actLevelStack.push(actCFANode);
      createLoopDrivenStateOrdering0(
          pAbstractionStates, loopLevelsToIndexMap, actLevelStack, loopStructure);
    }
  }

  private static void createLoopDrivenStateOrdering0(
      final List<AbstractState> pAbstractionStates,
      final Multimap<Integer, Integer> loopLevelsToIndexMap,
      Deque<CFANode> actLevelStack,
      LoopStructure loopStructure) {

    // we are finished with the computation
    if (loopLevelsToIndexMap.size() == pAbstractionStates.size()) {
      return;
    }

    final int actARGState = loopLevelsToIndexMap.size();
    final AbstractState lastState = pAbstractionStates.get(actARGState - 1);
    final AbstractState actState = pAbstractionStates.get(actARGState);
    @Nullable CFANode actCFANode = AbstractStates.extractLocation(actState);

    Iterator<CFANode> it = actLevelStack.descendingIterator();
    while (it.hasNext()) {
      checkNotNull(actCFANode, "node may be null and code needs to be fixed");
      CFANode lastLoopNode = it.next();

      // check if the functions match, if yes we can simply check if the node
      // is in the loop on this level, if not we have to check the functions entry
      // point, in order to know if the current node is in the loop on this
      // level or on a lower one
      if (actCFANode.getFunctionName().equals(lastLoopNode.getFunctionName())) {
        actCFANode =
            getPrevFunctionNode(
                (ARGState) actState, (ARGState) lastState, lastLoopNode.getFunctionName());
      }

      // the lastLoopNode cannot be reached from the actState
      // so decrease the actLevelStack
      if (actCFANode == null || !isNodePartOfLoop(lastLoopNode, actCFANode, loopStructure)) {
        it.remove();
        continue;

        // we have a valid path to the function of the lastLoopNode
      } else {
        loopLevelsToIndexMap.put(actLevelStack.size(), actARGState);

        // node itself is a loophead, too, so add it also to the levels stack
        if (loopStructure.getAllLoopHeads().contains(actCFANode)) {
          actLevelStack.push(actCFANode);
        }
        createLoopDrivenStateOrdering0(
            pAbstractionStates, loopLevelsToIndexMap, actLevelStack, loopStructure);
        return;
      }
    }

    // coming here is possible only if the stack is empty and no matching
    // loop for the current node was found
    createLoopDrivenStateOrdering(
        pAbstractionStates, loopLevelsToIndexMap, actLevelStack, loopStructure);
  }

  private static boolean isNodePartOfLoop(
      CFANode loopHead, CFANode potentialLoopNode, LoopStructure loopStructure) {
    for (Loop loop : loopStructure.getLoopsForLoopHead(loopHead)) {
      if (loop.getLoopNodes().contains(potentialLoopNode)) {
        return true;
      }
    }
    return false;
  }

  private static CFANode getPrevFunctionNode(
      ARGState argState, ARGState lastState, String wantedFunction) {
    CFANode returnNode = AbstractStates.extractLocation(argState);
    while (!returnNode.getFunctionName().equals(wantedFunction)) {
      argState = argState.getParents().iterator().next();

      // the function does not return to the wanted function we can skip the search here
      if (Objects.equals(argState, lastState.getParents().iterator().next())) {
        return null;
      }

      returnNode = AbstractStates.extractLocation(argState);
    }

    return returnNode;
  }
}
