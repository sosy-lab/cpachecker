/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

enum CexTraceAnalysisDirection {
  /**
   * Just the trace as it is
   */
  FORWARDS {
    @Override
    public ImmutableList<Triple<BooleanFormula, AbstractState, Integer>>
    orderFormulas(List<BooleanFormula> traceFormulas,
                  List<AbstractState> abstractionStates,
                  VariableClassification pVariableClassification,
                  LoopStructure pLoopStructure,
                  FormulaManagerView pFmgr) {
      Builder<Triple<BooleanFormula, AbstractState, Integer>> orderedFormulas = ImmutableList.builder();
      for (int i = 0; i < traceFormulas.size(); i++) {
        orderedFormulas.add(Triple.of(traceFormulas.get(i), abstractionStates.get(i), i));
      }
      return orderedFormulas.build();
    }
  },

  /**
   * The trace when traversed backwards
   */
  BACKWARDS {
    @Override
    public ImmutableList<Triple<BooleanFormula, AbstractState, Integer>>
    orderFormulas(List<BooleanFormula> traceFormulas,
                  List<AbstractState> abstractionStates,
                  VariableClassification pVariableClassification,
                  LoopStructure pLoopStructure,
                  FormulaManagerView pFmgr) {
      Builder<Triple<BooleanFormula, AbstractState, Integer>> orderedFormulas = ImmutableList.builder();
      for (int i = traceFormulas.size()-1; i >= 0; i--) {
        orderedFormulas.add(Triple.of(traceFormulas.get(i), abstractionStates.get(i), i));
      }
      return orderedFormulas.build();
    }
  },

  /**
   * Takes alternatingly one element from the front of the trace and one of
   * the back
   */
  ZIGZAG {
    @Override
    public ImmutableList<Triple<BooleanFormula, AbstractState, Integer>>
    orderFormulas(List<BooleanFormula> traceFormulas,
                  List<AbstractState> abstractionStates,
                  VariableClassification pVariableClassification,
                  LoopStructure pLoopStructure,
                  FormulaManagerView pFmgr) {
      Builder<Triple<BooleanFormula, AbstractState, Integer>> orderedFormulas = ImmutableList.builder();
      int e = traceFormulas.size() - 1;
      int s = 0;
      boolean fromStart = false;
      while (s <= e) {
        int i = fromStart ? s++ : e--;
        fromStart = !fromStart;

        orderedFormulas.add(Triple.of(traceFormulas.get(i), abstractionStates.get(i), i));
      }
      return orderedFormulas.build();
    }
  },

  /**
   * Those parts of the trace that are in no loops or in less loops than
   * others are sorted to the front
   */
  LOOP_FREE_FIRST {
    @Override
    public ImmutableList<Triple<BooleanFormula, AbstractState, Integer>>
    orderFormulas(List<BooleanFormula> traceFormulas,
                  List<AbstractState> abstractionStates,
                  VariableClassification pVariableClassification,
                  LoopStructure pLoopStructure,
                  FormulaManagerView pFmgr) {
      Builder<Triple<BooleanFormula, AbstractState, Integer>> orderedFormulas = ImmutableList.builder();
      Multimap<Integer, AbstractState> stateOrdering = LinkedHashMultimap.create();
      createLoopDrivenStateOrdering(abstractionStates,
                                    stateOrdering,
                                    new ArrayDeque<CFANode>(),
                                    checkNotNull(pLoopStructure));

      for (int i = 0; stateOrdering.containsKey(i); i++) {
        Collection<AbstractState> states = stateOrdering.get(i);
        for (AbstractState state : states) {
          int id = abstractionStates.indexOf(state);
          orderedFormulas.add(Triple.of(traceFormulas.get(id), state, id));
        }
      }
      return orderedFormulas.build();
    }
  },

  /**
   * A random order of the trace
   */
  RANDOM {
    @Override
    public ImmutableList<Triple<BooleanFormula, AbstractState, Integer>>
    orderFormulas(List<BooleanFormula> traceFormulas,
                  List<AbstractState> abstractionStates,
                  VariableClassification pVariableClassification,
                  LoopStructure pLoopStructure,
                  FormulaManagerView pFmgr) {
      Builder<Triple<BooleanFormula, AbstractState, Integer>> orderedFormulas = ImmutableList.builder();
      List<AbstractState> stateList = new ArrayList<>(abstractionStates);
      Collections.shuffle(stateList);

      for (int i = 0; i < traceFormulas.size(); i++) {
        AbstractState state = stateList.get(i);
        int oldIndex = abstractionStates.indexOf(state);
        orderedFormulas.add(Triple.of(traceFormulas.get(oldIndex), state, oldIndex));
      }
      return orderedFormulas.build();
    }
  },

  /**
   * Formulas with the lowest average score for their variables according
   * to some calculations in the VariableClassification are sorted to the front
   */
  LOWEST_AVG_SCORE {
    @Override
    public ImmutableList<Triple<BooleanFormula, AbstractState, Integer>>
    orderFormulas(List<BooleanFormula> traceFormulas,
                  List<AbstractState> abstractionStates,
                  VariableClassification pVariableClassification,
                  LoopStructure pLoopStructure,
                  FormulaManagerView pFmgr) {
      Builder<Triple<BooleanFormula, AbstractState, Integer>> orderedFormulas = ImmutableList.builder();
      Multimap<Double, Integer> sortedFormulas = TreeMultimap.create();

      for (BooleanFormula formula : traceFormulas) {
        if (!sortedFormulas.put(getAVGScoreForVariables(formula,
                                                        checkNotNull(pVariableClassification),
                                                        checkNotNull(pFmgr),
                                                        checkNotNull(pLoopStructure)),
                                traceFormulas.indexOf(formula))) {
          throw new AssertionError("Bug in creation of sorted formulas.");
        }
      }

      for (Integer index : sortedFormulas.values()) {
        orderedFormulas.add(Triple.of(traceFormulas.get(index),
                                      abstractionStates.get(index),
                                      index));
      }
      return orderedFormulas.build();
    }
  },

  /**
   * Formulas with the highest average score for their variables according
   * to some calculations in the VariableClassification are sorted to the front
   */
  HIGHEST_AVG_SCORE {
    @Override
    public ImmutableList<Triple<BooleanFormula, AbstractState, Integer>>
    orderFormulas(List<BooleanFormula> traceFormulas,
                  List<AbstractState> abstractionStates,
                  VariableClassification pVariableClassification,
                  LoopStructure pLoopStructure,
                  FormulaManagerView pFmgr) {
      Builder<Triple<BooleanFormula, AbstractState, Integer>> orderedFormulas = ImmutableList.builder();
      Multimap<Double, Integer> sortedFormulas = TreeMultimap.create();

      for (BooleanFormula formula : traceFormulas) {
        if (!sortedFormulas.put(Double.MAX_VALUE - getAVGScoreForVariables(formula,
                                                                           checkNotNull(pVariableClassification),
                                                                           checkNotNull(pFmgr),
                                                                           checkNotNull(pLoopStructure)),
                                traceFormulas.indexOf(formula))) {
          throw new AssertionError("Bug in creation of sorted formulas.");
        }
      }

      for (Integer index : sortedFormulas.values()) {
        orderedFormulas.add(Triple.of(traceFormulas.get(index),
                                      abstractionStates.get(index),
                                      index));
      }
      return orderedFormulas.build();
    }
  },

  /**
   * Combination of loop free first and backwards, for each loop level, we iterate
   * backwards through the found formulas to have those that are closest to
   * the error location at first.
   */
  LOOP_FREE_FIRST_BACKWARDS {
    @Override
    public ImmutableList<Triple<BooleanFormula, AbstractState, Integer>>
          orderFormulas(List<BooleanFormula> traceFormulas,
                        List<AbstractState> abstractionStates,
                        VariableClassification pVariableClassification,
                        LoopStructure pLoopStructure,
                        FormulaManagerView pFmgr) {
      Builder<Triple<BooleanFormula, AbstractState, Integer>> orderedFormulas = ImmutableList.builder();
      Multimap<Integer, AbstractState> stateOrdering = LinkedHashMultimap.create();
      createLoopDrivenStateOrdering(abstractionStates,
                                    stateOrdering,
                                    new ArrayDeque<CFANode>(),
                                    checkNotNull(pLoopStructure));

      for (int i = 0; stateOrdering.containsKey(i); i++) {
        Collection<AbstractState> stateSet = stateOrdering.get(i);
        AbstractState[] stateArray = new AbstractState[stateSet.size()];
        stateSet.toArray(stateArray);
        for (int j = stateArray.length-1; j >= 0; j--) {
          int id = abstractionStates.indexOf(stateArray[j]);
          orderedFormulas.add(Triple.of(traceFormulas.get(id), stateArray[j], id));
        }
      }
      return orderedFormulas.build();
    }
  };

  /**
   * This method reorders the given formulas such that their order applies to the
   * name of the given enum constant.
   * @return The reordered formulas
   */
  public abstract ImmutableList<Triple<BooleanFormula, AbstractState, Integer>>
                    orderFormulas(List<BooleanFormula> traceFormulas,
                                  List<AbstractState> abstractionStates,
                                  VariableClassification variableClassification,
                                  LoopStructure loopStructure,
                                  FormulaManagerView fmgr);

  /**
   * This method computes a score for a set of variables regarding the domain
   * types of these variables.
   * @return the average score over all given variables
   */
  private static double getAVGScoreForVariables(BooleanFormula formula,
                                                VariableClassification variableClassification,
                                                FormulaManagerView fmgr,
                                                LoopStructure loopStructure) {

    Set<String> varNames = from(fmgr.extractVariableNames(formula))
        .transform(new Function<String, String>() {
           @Override
           public String apply(String pInput) {
             Pair<String, OptionalInt> name = FormulaManagerView.parseName(pInput);

            // we want only variables to be in our set, and ignore everything without SSA index
             if (name.getSecond().isPresent()) {
               return name.getFirst();
             } else {
               return null;
             }
           }})
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
      if(currentScore < 0) {
        return Double.MAX_VALUE / varNames.size();
      }
    }

    // this is a true or false formula, return 0 as this is the easiest formula
    // we can encounter
    if (varNames.size() == 0) {
      return 0;
    } else {
      return currentScore / varNames.size();
    }
  }

  private static void createLoopDrivenStateOrdering(final List<AbstractState> pAbstractionStates,
                                                    final Multimap<Integer, AbstractState> loopLevelsToStatesMap,
                                                    Deque<CFANode> actLevelStack,
                                                    LoopStructure loopStructure) {
    ImmutableSet<CFANode> loopHeads = loopStructure.getAllLoopHeads();

    // in the nodeLoopLevel map there has to be for every seen ARGState one
    // key-value pair therefore we can use this as our index
    int actARGState = loopLevelsToStatesMap.size();

    AbstractState actState = null;
    CFANode actCFANode = null;

    boolean isCFANodeALoopHead = false;

    // move on as long as there occurs no loop-head in the ARG path
    while (!isCFANodeALoopHead
           && actLevelStack.isEmpty()
           && actARGState < pAbstractionStates.size()) {

      actState = pAbstractionStates.get(actARGState);
      actCFANode = AbstractStates.extractLocation(actState);

      loopLevelsToStatesMap.put(0, actState);

      isCFANodeALoopHead = loopHeads.contains(actCFANode);

      actARGState++;
    }

    // when not finished with computing the node levels
    if (actARGState != pAbstractionStates.size()) {
      actLevelStack.push(actCFANode);
      createLoopDrivenStateOrdering0(pAbstractionStates, loopLevelsToStatesMap, actLevelStack, loopStructure);
    }
  }

  private static void createLoopDrivenStateOrdering0(final List<AbstractState> pAbstractionStates,
                                                     final Multimap<Integer, AbstractState> loopLevelsToStatesMap,
                                                     Deque<CFANode> actLevelStack,
                                                     LoopStructure loopStructure) {

    // we are finished with the computation
    if (loopLevelsToStatesMap.size() == pAbstractionStates.size()) {
      return;
    }

    AbstractState lastState = pAbstractionStates.get(loopLevelsToStatesMap.size()-1);
    AbstractState actState = pAbstractionStates.get(loopLevelsToStatesMap.size());
    CFANode actCFANode = AbstractStates.extractLocation(actState);

    Iterator<CFANode> it = actLevelStack.descendingIterator();
    while (it.hasNext()) {
      CFANode lastLoopNode = it.next();

      // check if the functions match, if yes we can simply check if the node
      // is in the loop on this level, if not we have to check the functions entry
      // point, in order to know if the current node is in the loop on this
      // level or on a lower one
      if (actCFANode.getFunctionName().equals(lastLoopNode.getFunctionName())) {
        actCFANode = getPrevFunctionNode((ARGState)actState,
                                         (ARGState)lastState,
                                         lastLoopNode.getFunctionName());
      }

      // the lastLoopNode cannot be reached from the actState
      // so decrease the actLevelStack
      if (actCFANode == null
          || !isNodePartOfLoop(lastLoopNode, actCFANode, loopStructure)) {
        it.remove();
        continue;

        // we have a valid path to the function of the lastLoopNode
      } else {
        loopLevelsToStatesMap.put(actLevelStack.size(), actState);

        // node itself is a loophead, too, so add it also to the levels stack
        if (loopStructure.getAllLoopHeads().contains(actCFANode)) {
          actLevelStack.push(actCFANode);
        }
        createLoopDrivenStateOrdering0(pAbstractionStates, loopLevelsToStatesMap, actLevelStack, loopStructure);
        return;
      }
    }

    // coming here is possible only if the stack is empty and no matching
    // loop for the current node was found
    createLoopDrivenStateOrdering(pAbstractionStates, loopLevelsToStatesMap, actLevelStack, loopStructure);
  }

  private static boolean isNodePartOfLoop(CFANode loopHead, CFANode potentialLoopNode, LoopStructure loopStructure) {
    for (Loop loop : loopStructure.getLoopsForLoopHead(loopHead)) {
      if (loop.getLoopNodes().contains(potentialLoopNode)) {
        return true;
      }
    }
    return false;
  }

  private static CFANode getPrevFunctionNode(ARGState argState, ARGState lastState, String wantedFunction) {
    CFANode returnNode = AbstractStates.extractLocation(argState);
    while (!returnNode.getFunctionName().equals(wantedFunction)) {
      argState = argState.getParents().iterator().next();

      // the function does not return to the wanted function we can skip the search
      // here
      if (argState == lastState.getParents().iterator().next()) {
        return null;
      }

      returnNode = AbstractStates.extractLocation(argState);
    }

    return returnNode;
  }

}