// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.uni_freiburg.informatik.ultimate.core.lib.translation.DefaultTranslator.IFunction;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.join.SMGMergeStatus;
import org.sosy_lab.cpachecker.util.smg.util.MergedSMGStateAndMergeStatus;

public class SMGMergeTest extends SMGCPATest0 {

  List<SMGState> reached = new ArrayList<>();

  private static final String TOP_LIST_STACK_VARIABLE_1 = "testStackVariableTopList1";
  private static final String TOP_LIST_STACK_VARIABLE_2 = "testStackVariableTopList2";
  private static final String NESTED_LIST_STACK_VARIABLE_1 = "testStackVariableNestedList1";

  // Tests merge for SLL with a pointer from a stack variable towards the beginning
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeBareSLLWithPointerTowardsBeginning() throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec = ListSpec::getSllWithNoValues;

    generateListsAndAssertMergeability(
        false,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 1,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for SLL with a pointer from a stack variable towards the beginning
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeSLLWithZeroValueAndPointerTowardsBeginning()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec =
        ListSpec::getSllWithSingleZeroValueBeforeNfo;

    generateListsAndAssertMergeability(
        false,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 1,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for SLL with a pointer from a stack variable towards the beginning
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeSLLWithZeroValueAndPointerTowardsBeginning2()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec =
        ListSpec::getSllWithSingleZeroValueAfterNfo;

    generateListsAndAssertMergeability(
        false,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 1,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for DLL with a pointer from a stack variable towards the beginning
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeBareDLLWithPointerTowardsBeginning() throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec = ListSpec::getDllWithNoValues;

    generateListsAndAssertMergeability(
        true,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 1,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for DLL with a pointer from a stack variable towards the beginning
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeBareDLLWithPointerTowardsBeginningAndEnd()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0, TOP_LIST_STACK_VARIABLE_2, Integer.MAX_VALUE);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec = ListSpec::getDllWithNoValues;

    generateListsAndAssertMergeability(
        true,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 2,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for SLL with each element having an identical nondet value with a pointer from a
  // stack variable towards the beginning.
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeSLLWithIdenticalNondetValuesAndPointerTowardsBeginning()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec =
        ListSpec::getSllWithSingleIdenticalNondetIntValueBeforeNfoAndPfo;

    generateListsAndAssertMergeability(
        false,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 1,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for SLL with each element having a distinct nondet value with a pointer from a
  // stack variable towards the beginning.
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeSLLWithDistinctNondetValuesAndPointerTowardsBeginning()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec =
        ListSpec::getSllWithSingleDistinctNondetIntValueBeforeNfoAndPfo;

    generateListsAndAssertMergeability(
        false,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 1,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for DLL with each element having an identical nondet value with a pointer from a
  // stack variable towards the beginning.
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeDLLWithIdenticalNondetValuesAndPointerTowardsBeginning()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec =
        ListSpec::getDllWithSingleIdenticalNondetIntValueBeforeNfoAndPfo;

    generateListsAndAssertMergeability(
        true,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 1,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for DLL with each element having an identical nondet value with a pointer from a
  // stack variable towards the beginning and end of the list.
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeDLLWithIdenticalNondetValuesAndPointerTowardsBeginningAndEnd()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0, TOP_LIST_STACK_VARIABLE_2, Integer.MAX_VALUE);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec =
        ListSpec::getDllWithSingleIdenticalNondetIntValueBeforeNfoAndPfo;

    generateListsAndAssertMergeability(
        true,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 2,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for DLL with each element having a distinct nondet value with a pointer from a
  // stack variable towards the beginning.
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeDLLWithDistinctNondetValuesAndPointerTowardsBeginning()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec =
        ListSpec::getDllWithSingleDistinctNondetIntValueBeforeNfoAndPfo;

    generateListsAndAssertMergeability(
        true,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 1,
        spec,
        variableAndPointerLocationInList);
  }

  // Tests merge for DLL with each element having a distinct nondet value with a pointer from a
  // stack variable towards the beginning and end of the list.
  // Tests ALL permutations needed to fully subsume a list.
  @Test
  public void mergeDLLWithDistinctNondetValuesAndPointerTowardsBeginningAndEnd()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0, TOP_LIST_STACK_VARIABLE_2, Integer.MAX_VALUE);
    BiFunction<Integer, Map<String, Integer>, ListSpec> spec =
        ListSpec::getDllWithSingleDistinctNondetIntValueBeforeNfoAndPfo;

    generateListsAndAssertMergeability(
        true,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 2,
        spec,
        variableAndPointerLocationInList);
  }

  // ############################# Nested List Tests Below This Point #############################

  // Tests merge for DLL with a top-list with exactly 1 element having a nested list of length 1 to
  // minimal abstraction length + 2. Each nested list element having an identical nondet value with
  // a pointer from a stack variable towards the beginning of the top list and a single pointer
  // towards the beginning of the nested list that is shared between the top list and a stack
  // variable.
  @Test
  public void mergeDLLWithSingleNestedListWithIdenticalNondetValuesAndPointerTowardsBeginnings()
      throws CPAException, InterruptedException {
    Map<String, Integer> variableAndPointerLocationInList =
        ImmutableMap.of(TOP_LIST_STACK_VARIABLE_1, 0);
    IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec> spec =
        ListSpec::getBareDllWithNestedListBeforeNextAndPrevOf;
    BiFunction<Integer, Map<String, Integer>, ListSpec> nestedSpec =
        ListSpec::getDllWithSingleIdenticalNondetIntValueBeforeNfoAndPfo;

    generateListsWithSingleNestedListsAndAssertMergeability(
        true,
        true,
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 2,
        spec,
        variableAndPointerLocationInList,
        nestedSpec,
        NESTED_LIST_STACK_VARIABLE_1);
  }

  @Test
  public void
      mergeSLLWithPointerTowardsBeginningWithNestedListWithIdenticalNondetValuesAndPointerTowardsEndOfNestedDFS()
          throws CPAException, InterruptedException {
    int maxLength =
        smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold() + 2;
    Optional<String> variableTowardsFirstTopElement = Optional.of(TOP_LIST_STACK_VARIABLE_1);
    Optional<String> variableTowardsLastTopElement = Optional.of(TOP_LIST_STACK_VARIABLE_2);
    Optional<String> variableTowardsLastNested = Optional.of(NESTED_LIST_STACK_VARIABLE_1);

    IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec> spec =
        ListSpec::getBareSllWithNestedListBeforeNextOf;

    IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec> nestedSpec =
        ListSpec::getSllWithSingleIdenticalNondetIntValueAfterNfo;

    generateAndAssertMergeabilityAndSubsumtionOfListSpecifiedInCPAcheckerOrder(
        false,
        maxLength,
        variableTowardsFirstTopElement,
        variableTowardsLastTopElement,
        spec,
        variableTowardsLastNested,
        ImmutableList.of(nestedSpec));
  }

  // ####################### No more tests, just generators/helper methods #######################

  /**
   * Generates the list according to the specified list parameters with all possible nested lists
   * and asserts that it is abstractable and subsequently mergeable. Assumes that the given list is
   * not mergeable at first, but abstractable and subsequently mergeable with the abstracted states.
   * The nestedListStackVariable pointer points to the last nested list element, while the top-list
   * points to the first.
   *
   * @param nestedListStackVariable name of the var pointing to the nested list.
   */
  private void generateListsWithSingleNestedListsAndAssertMergeability(
      boolean dll,
      boolean nestedListHasStackPointerAtTheEnd,
      int maxListLength,
      IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec> spec,
      Map<String, Integer> variableAndPointerLocationInList,
      BiFunction<Integer, Map<String, Integer>, ListSpec> nestedListSpec,
      String nestedListStackVariable)
      throws CPAException, InterruptedException {
    List<List<List<SMGState>>> statesToTestWithOneNestedList =
        generateListsWithSingleNestedListsFromSpec(
            spec,
            variableAndPointerLocationInList,
            nestedListSpec,
            nestedListStackVariable,
            nestedListHasStackPointerAtTheEnd,
            maxListLength);

    int topListLength = 1;
    for (List<List<SMGState>> topListsByLength : statesToTestWithOneNestedList) {
      int indexOfNestedList = 1;
      for (List<SMGState> nestedListsToTest : topListsByLength) {
        // Check that they are not mergeable before abstraction
        assertNotMergeable(nestedListsToTest);

        // Abstract (only viable states are abstracted)
        List<SMGState> statesToTestWithAbstraction =
            abstractAbstractableStatesWithSingleNestedLists(
                nestedListsToTest, dll, maxListLength, topListLength, indexOfNestedList);

        int minAbstrLen =
            smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold();
        boolean topListAbstracted =
            topListLength - indexOfNestedList >= minAbstrLen || indexOfNestedList > minAbstrLen;

        // Merge now succeed with the abstracted state(s)
        if (topListAbstracted) {
          assertMergeOfSingleNestedListsSucceedsWithAllAbstractedStates(
              statesToTestWithAbstraction, topListAbstracted);
        } else {
          assertMergeSucceedsWithAllAbstractedStates(statesToTestWithAbstraction);
        }
        indexOfNestedList++;
      }
      topListLength++;
    }
  }

  /**
   * @param listLengthToAbortTest if at this length the list can not be abstracted or merged, the
   *     test is aborted.
   * @param topListSpecGen specification of the uppermost list. Must be present.
   * @param nestedListSpecsByNesting specifies nested lists by nesting in the list.
   */
  @SuppressWarnings("all")
  private void generateAndAssertMergeabilityAndSubsumtionOfListSpecifiedInCPAcheckerOrder(
      boolean bfsTrueAndDfsFalse,
      int listLengthToAbortTest,
      Optional<String> variableTowardsFirstTopElement,
      Optional<String> variableTowardsLastTopElement,
      IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec> topListSpecGen,
      Optional<String> variableTowardsLatestNestedElement,
      List<IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec>>
          nestedListSpecsByNesting)
      throws CPAException, InterruptedException {

    SMGState initialState = currentState;
    reached = new ArrayList<>();
    // Make warning go away
    // TODO: delete once used
    assertThat(reached.isEmpty()).isTrue();

    // TODO: implement BFS
    assertThat(bfsTrueAndDfsFalse).isFalse();

    /*
    ImmutableMap.Builder<String, Integer> variableAndPointerLocationInListBuilder =
        ImmutableMap.builder();
    if (variableTowardsFirstTopElement.isPresent()) {
      variableAndPointerLocationInListBuilder.put(variableTowardsFirstTopElement.orElseThrow(), 0);
    }
    if (variableTowardsLastTopElement.isPresent()) {
      variableAndPointerLocationInListBuilder.put(
          variableTowardsLastTopElement.orElseThrow(), Integer.MAX_VALUE);
    }
    Map<String, Integer> variableAndPointerLocationInList =
        variableAndPointerLocationInListBuilder.buildOrThrow();
        */

    /*
    int len = 1;
    boolean stop = false;
    while (!stop) {

      // Nested null, as it is replaced later on
      ListSpec topListSpec = topListSpecGen.create(len, variableAndPointerLocationInList, null);
      stop =
          buildListsInDFSOrderAndAssertWithPseudoCPA(
              listLengthToAbortTest,
              nestedListSpecsByNesting.getFirst(),
              variableTowardsLatestNestedElement,
              nestedListSpecsByNesting.subList(1, nestedListSpecsByNesting.size()),
              topListSpec,
              len);

      assertThat(len).isLessThan(listLengthToAbortTest);
      len++;

    }*/

    currentState = initialState;
  }

  /* TODO: make this javadoc again once commented out code is back
   * Returns the result of the STOP operator, i.e. false for continue, true for stop. The current
   * list should not be extended with new elements once true is returned. Will perform pseudo CPA
   * (abstract/precision adjustment, merge, stop).
   */
  /*
    private boolean performPseudoCPA(
        SMGState newStateWithList)
        throws CPAException, InterruptedException {

      // Try to abstract, add result to reached
      SMGState maybeNewAbstractedState = abstractListsWithDefaultOptions(newStateWithList);

      // Try to merge abstracted with all in reached and replace the taken with the merged.
      List<SMGState> removeFromReached = new ArrayList<>();
      List<SMGState> addToReached = new ArrayList<>();
      for (SMGState stateFromReached : reached) {
        Optional<MergedSMGStateAndMergeStatus> mergeResult =
            mergeOp.mergeForTests(maybeNewAbstractedState, stateFromReached);

        if (mergeResult.isPresent()) {
          SMGState mergedState = mergeResult.orElseThrow().getMergedSMGState();
          removeFromReached.add(stateFromReached);
          addToReached.add(mergedState);
        }
      }

      reached.removeAll(removeFromReached);
      reached.addAll(addToReached);

      // Try <= on all in reached, it should only work for successfully merge or abstraction cases
      // at all.
      // If <=, stop generation into length of this combination
      for (SMGState stateFromReached : reached) {
        if (maybeNewAbstractedState.isLessOrEqual(stateFromReached)) {
          // STOP
          return true;
        }
      }

      reached.add(maybeNewAbstractedState);

      return false;
    }

    private boolean buildListsInDFSOrderAndAssertWithPseudoCPA(
        int listLengthToAbortTest,
        IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec> listSpecToApply,
        Optional<String> variableTowardsLatestNestedElement,
        List<IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec>> nestedListSpecsToApply, ListSpec currentSpecification, int lengthOfLastInCurrent)
        throws CPAException, InterruptedException {

      // Error in test-setup
      assertThat(currentSpecification).isNotNull();
      // We don't support nesting beyond 2 nestings for now (and we split 1 of into listSpecToApply)
      assertThat(nestedListSpecsToApply.size()).isLessThan(2);

      boolean stop = false;
      Map<String, Integer> variableMap = ImmutableMap.of();
      if (nestedListSpecsToApply.isEmpty()) {
        if (variableTowardsLatestNestedElement.isPresent()) {
          variableMap =
              ImmutableMap.of(variableTowardsLatestNestedElement.orElseThrow(), Integer.MAX_VALUE);
        }

        // Generate x specs/states into depth with all other indices null until stop
        // Then, for all generated specs/states, do the same at the next index etc
        for (int index = 0; index < lengthOfLastInCurrent; index++) {
        ImmutableList.Builder<ListSpec> specs = ImmutableList.builder();
        int lengthNested = 0;
        while (!stop) {
          assertThat(lengthNested).isLessThan(listLengthToAbortTest);
          ListSpec nestedListSpec;
          // For 0 we leave the null in current so that no nested is created
          if (lengthNested != 0) {
           nestedListSpec = listSpecToApply.create(lengthNested, variableMap, null);
            // TODO: combine with prev
            ImmutableList.Builder<ListSpec> newNestedLists = ImmutableList.builder();
            newNestedLists.add(nestedListSpec);
            if (index > 0) {
              // Add more to newNestedLists up until index
              // addAnotherElementToList
            }

            currentSpecification.replaceNestedListSpecs(newNestedLists.build());
          }


          SMGState stateWithNestedList = buildConcreteListWith(currentSpecification);
          stop = performPseudoCPA(stateWithNestedList);
          lengthNested++;
        }
        }
        // return specs;

      } else {
        IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec> nextNestedSpec =
            nestedListSpecsToApply.getFirst();
        List<IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec>> remainingNested =
            nestedListSpecsToApply.subList(1, nestedListSpecsToApply.size());
        // TODO: support more nestings?
        assertThat(remainingNested).isEmpty();

        throw new AssertionError("Implement me");
      }

      return stop;
    }

    // Only DFS for now
    private <A> void getAllCombinationsFor(
        Set<A> specs, int breite, int length, int maxWidth, List<A> current, List<List<A>> result) {
      if (current.size() == maxWidth) {
        result.add(current);

      } else {

        Iterator<A> specIter = specs.iterator();
        for (int i = -1; i < specs.size(); i++) {
          A spec;
          if (i == -1) {
            spec = null;
          } else {
            spec = specIter.next();
          }
          ArrayList<A> newCurrent = new ArrayList<>();
          newCurrent.addAll(current);
          newCurrent.add(spec);
        }
      }
    }

    private void dfs(List<Integer> l, List<Integer> temp, int idx) {
      // Print the current combination

      // Base case: if idx exceeds the list size, return
      if (idx >= l.size()) {
        return;
      }

      // Iterate through the remaining elements starting from idx
      for (int i = idx; i < l.size(); i++) {
        // Add the current element to the temp list
        temp.add(l.get(i));

        // Recursively generate combinations with the next index
        dfs(l, temp, i + 1);

        // Backtrack by removing the last added element
        temp.remove(temp.size() - 1);
      }
    }
  */

  /**
   * Generates the list according to the specified list parameters and asserts that it is
   * abstractable and subsequently mergeable. Assumes that the given list is not mergeable at first,
   * but abstractable and subsequently mergeable with the abstracted states.
   */
  private void generateListsAndAssertMergeability(
      boolean dll,
      int maxListLength,
      BiFunction<Integer, Map<String, Integer>, ListSpec> spec,
      Map<String, Integer> variableAndPointerLocationInList)
      throws CPAException, InterruptedException {
    assertThat(variableAndPointerLocationInList.size()).isAtLeast(1);
    assertThat(variableAndPointerLocationInList.size()).isAtMost(2);

    List<SMGState> statesToTest =
        generateListsFromSpec(spec, variableAndPointerLocationInList, maxListLength);

    // Check that they are not mergeable before abstraction
    assertNotMergeable(statesToTest);

    // Abstract (only viable states are abstracted)
    List<SMGState> statesToTestWithAbstraction =
        abstractAbstractableStates(statesToTest, dll, maxListLength);

    // Merge now succeed with the abstracted state(s)
    assertMergeSucceedsWithAllAbstractedStates(statesToTestWithAbstraction);
  }

  /**
   * Asserts that all states that have abstracted list elements in them are mergeable with all other
   * states, and that the resulting states themselves are mergeable, until only 1 state remains that
   * subsumes all original and intermediate states.
   *
   * <p>concrete list length == CLL Note: the order in which CPAchecker generates and merges states
   * is (example): From left to right: CLL1, CLL2, S/DLL3 S/DLL3 is then merged with CLL1 AND CLL2
   * (as they are in reached), the newly merged states S/DLL1 and S/DLL2 replace CLL1 and CLL2 in
   * reached, and S/DLL3 is subsumed by any of the 2 in stop.
   */
  private SMGState assertMergeSucceedsWithAllAbstractedStates(
      List<SMGState> statesToTestWithAbstraction) throws CPAException, InterruptedException {
    // Assert mergeability of abstracted states with abstracted states
    SMGState abstractedState = assertSuccessfulMergeOfAbstractedStates(statesToTestWithAbstraction);

    List<SMGState> allNonAbstractedStates =
        statesToTestWithAbstraction.stream()
            .filter(s -> s.getMemoryModel().getSmg().getNumberOfAbstractedLists() == 0)
            .collect(ImmutableList.toImmutableList());

    // All non-abstracted states should be mergeable with abstracted states.
    // They should also be mergeable with the previously merged state.
    SMGState smallestAbstractedState = abstractedState;
    for (int i = allNonAbstractedStates.size() - 1; i >= 0; i--) {
      SMGState stateToMerge = allNonAbstractedStates.get(i);
      boolean twoStackVarsPointToTheSameObj =
          twoStackVariablesPointToTheSameConcreteListSegment(stateToMerge);
      // Skip states if there is 2 stack variables with pointers pointing to the same
      //  non-abstracted list segment, as they can't be abstracted currently.
      if (smallestAbstractedState != abstractedState && !twoStackVarsPointToTheSameObj) {
        // All non-abstracted states should be mergeable with abstracted states.
        Optional<MergedSMGStateAndMergeStatus> mergeResult =
            mergeOp.mergeForTests(abstractedState, stateToMerge);

        assertThat(mergeResult).isPresent();
        assertThat(mergeResult.orElseThrow().getMergeStatus())
            .isEqualTo(SMGMergeStatus.RIGHT_ENTAILED_IN_LEFT);
        assertThat(abstractedState.isLessOrEqual(mergeResult.orElseThrow().getMergedSMGState()))
            .isTrue();
        smallestAbstractedState = mergeResult.orElseThrow().getMergedSMGState();
      }
    }
    return smallestAbstractedState;
  }

  private SMGState assertMergeOfSingleNestedListsSucceedsWithAllAbstractedStates(
      List<SMGState> statesToTestWithAbstraction, boolean topListAbstracted)
      throws CPAException, InterruptedException {
    // Assert mergeability of abstracted states with abstracted states
    SMGState abstractedState =
        assertSuccessfulMergeOfAbstractedStatesWithSingleNestedList(
            statesToTestWithAbstraction, topListAbstracted);

    List<SMGState> allNonAbstractedStates =
        statesToTestWithAbstraction.stream()
            .filter(s -> s.getMemoryModel().getSmg().getNumberOfAbstractedLists() == 0)
            .collect(ImmutableList.toImmutableList());

    // All non-abstracted states should be mergeable with abstracted states.
    // They should also be mergeable with the previously merged state.
    SMGState smallestAbstractedState = abstractedState;
    for (int i = allNonAbstractedStates.size() - 1; i >= 0; i--) {
      SMGState stateToMerge = allNonAbstractedStates.get(i);
      boolean twoStackVarsPointToTheSameObj =
          twoStackVariablesPointToTheSameConcreteListSegment(stateToMerge);
      // Skip states if there is 2 stack variables with pointers pointing to the same
      //  non-abstracted list segment, as they can't be abstracted currently.
      if (smallestAbstractedState != abstractedState && !twoStackVarsPointToTheSameObj) {
        // All non-abstracted states should be mergeable with abstracted states.
        Optional<MergedSMGStateAndMergeStatus> mergeResult =
            mergeOp.mergeForTests(abstractedState, stateToMerge);

        assertThat(mergeResult).isPresent();
        assertThat(mergeResult.orElseThrow().getMergeStatus())
            .isEqualTo(SMGMergeStatus.RIGHT_ENTAILED_IN_LEFT);
        assertThat(abstractedState.isLessOrEqual(mergeResult.orElseThrow().getMergedSMGState()))
            .isTrue();
        smallestAbstractedState = mergeResult.orElseThrow().getMergedSMGState();
      }
    }
    return smallestAbstractedState;
  }

  private boolean twoStackVariablesPointToTheSameConcreteListSegment(SMGState state)
      throws SMGException {
    Map<String, SMGObject> stackVariables =
        state.getMemoryModel().getStackFrames().peek().getVariables();
    if (stackVariables.size() > 1) {
      Set<SMGObject> targets = new HashSet<>();
      for (SMGObject stackVariableObj : stackVariables.values()) {
        Value possiblePointer =
            state
                .readValueWithoutMaterialization(
                    stackVariableObj,
                    BigInteger.ZERO,
                    pointerSizeInBits,
                    CPointerType.POINTER_TO_VOID)
                .getValue();
        if (state.getMemoryModel().isPointer(possiblePointer)) {
          targets.add(state.getPointsToTarget(possiblePointer).orElseThrow().getSMGObject());
        }
      }

      checkState(!targets.isEmpty());
      // DLLs can have fst and last pointers. The test-setup should make sure that there is never 2
      // times the same spec.
      return targets.size() == 1 && targets.iterator().next() instanceof SMGDoublyLinkedListSegment;
    }
    return false;
  }

  /**
   * Asserts the mergeablity of all states with list abstractions in input list and returns the
   * state with the abstracted list that is the largest (i.e. smallest min length).
   */
  private static SMGState assertSuccessfulMergeOfAbstractedStates(
      List<SMGState> statesToTestWithAbstraction) throws CPAException, InterruptedException {
    List<SMGState> allAbstractedStates =
        statesToTestWithAbstraction.stream()
            .filter(s -> s.getMemoryModel().getSmg().getNumberOfAbstractedLists() > 0)
            .collect(ImmutableList.toImmutableList());
    assertThat(allAbstractedStates.size()).isGreaterThan(1);

    // Assert that they are ordered ascending
    for (int i = 0; i < allAbstractedStates.size() - 1; i++) {
      Set<SMGSinglyLinkedListSegment> abstrObjs =
          allAbstractedStates.get(i).getMemoryModel().getSmg().getAllValidAbstractedObjects();
      Set<SMGSinglyLinkedListSegment> abstrObjsNext =
          allAbstractedStates.get(i + 1).getMemoryModel().getSmg().getAllValidAbstractedObjects();
      assertThat(abstrObjs).hasSize(1);
      assertThat(abstrObjsNext).hasSize(1);
      assertThat(abstrObjsNext.iterator().next().getMinLength())
          .isGreaterThan(abstrObjs.iterator().next().getMinLength());
    }

    SMGState smallestAbstractedState = allAbstractedStates.getLast();
    for (int i = allAbstractedStates.size() - 2; i >= 0; i--) {
      SMGState stateToMergeRight = allAbstractedStates.get(i);
      Optional<MergedSMGStateAndMergeStatus> mergeRes =
          mergeOp.mergeForTests(smallestAbstractedState, stateToMergeRight);
      assertThat(mergeRes).isPresent();
      // Right is bigger (by having a smaller min length), hence it entails
      assertThat(mergeRes.orElseThrow().getMergeStatus())
          .isEqualTo(SMGMergeStatus.LEFT_ENTAILED_IN_RIGHT);

      assertThat(smallestAbstractedState.isLessOrEqual(mergeRes.orElseThrow().getMergedSMGState()))
          .isTrue();
    }
    return allAbstractedStates.getFirst();
  }

  /**
   * Asserts the mergeablity of all states with list abstractions in input list and returns the
   * state with the abstracted list that is the largest (i.e. smallest min length).
   */
  private static SMGState assertSuccessfulMergeOfAbstractedStatesWithSingleNestedList(
      List<SMGState> statesToTestWithAbstraction, boolean topListAbstracted)
      throws CPAException, InterruptedException {
    List<SMGState> allAbstractedStates =
        statesToTestWithAbstraction.stream()
            .filter(
                s ->
                    s.getMemoryModel().getSmg().getNumberOfAbstractedLists()
                        > (topListAbstracted ? 1 : 0))
            .collect(ImmutableList.toImmutableList());
    assertThat(allAbstractedStates.size()).isGreaterThan(1);

    // Assert that they are ordered ascending
    for (int i = 0; i < allAbstractedStates.size() - 1; i++) {
      Set<SMGSinglyLinkedListSegment> abstrObjs =
          allAbstractedStates.get(i).getMemoryModel().getSmg().getAllValidAbstractedObjects();
      Set<SMGSinglyLinkedListSegment> abstrObjsNext =
          allAbstractedStates.get(i + 1).getMemoryModel().getSmg().getAllValidAbstractedObjects();
      assertThat(abstrObjs).hasSize((topListAbstracted ? 2 : 1));
      assertThat(abstrObjsNext).hasSize((topListAbstracted ? 2 : 1));
      assertThat(abstrObjsNext.stream().mapToInt(SMGSinglyLinkedListSegment::getMinLength).sum())
          .isGreaterThan(
              abstrObjs.stream().mapToInt(SMGSinglyLinkedListSegment::getMinLength).sum());
    }

    SMGState smallestAbstractedState = allAbstractedStates.getLast();
    for (int i = allAbstractedStates.size() - 2; i >= 0; i--) {
      SMGState stateToMergeRight = allAbstractedStates.get(i);
      Optional<MergedSMGStateAndMergeStatus> mergeRes =
          mergeOp.mergeForTests(smallestAbstractedState, stateToMergeRight);
      assertThat(mergeRes).isPresent();
      // Right is bigger (by having a smaller min length), hence it entails
      assertThat(mergeRes.orElseThrow().getMergeStatus())
          .isEqualTo(SMGMergeStatus.LEFT_ENTAILED_IN_RIGHT);

      assertThat(smallestAbstractedState.isLessOrEqual(mergeRes.orElseThrow().getMergedSMGState()))
          .isTrue();
    }
    return allAbstractedStates.getFirst();
  }

  /**
   * Abstracts all viable states, i.e. all elements that are comparable and of min subsequent
   * length. All other states are returned as is. There is a "top" list with 1 to maxListLength
   * elements. One of those has a nested list with 1 to maxListLength elements. The others point to
   * 0 where the nested list pointer is located. Note: a top list might be partially abstracted if
   * the space surrounding the nested list source allows it!
   */
  private List<SMGState> abstractAbstractableStatesWithSingleNestedLists(
      List<SMGState> statesToTest,
      boolean dll,
      int maxListLength,
      int topListLength,
      int indexOfNestedListStartingFrom1)
      throws CPAException {
    ImmutableList.Builder<SMGState> statesToTestIncludingAbstractedStatesBuilder =
        ImmutableList.builder();

    // We can only abstract the top list if the nested (distinct) list element leaves enough room
    int marginLeft = topListLength - indexOfNestedListStartingFrom1;
    int minAbstrLen = smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold();
    boolean topListAbstracted =
        marginLeft >= minAbstrLen || indexOfNestedListStartingFrom1 > minAbstrLen;

    for (int numOfState = 0; numOfState < statesToTest.size(); numOfState++) {
      SMGState stateToBeAbstracted = statesToTest.get(numOfState);

      SMGState resultState = abstractListsWithDefaultOptions(stateToBeAbstracted);
      statesToTestIncludingAbstractedStatesBuilder.add(resultState);

      int numOfAbstrLists = topListAbstracted ? 1 : 0;
      if (numOfState + 1 >= maxListLength - 1) {
        numOfAbstrLists++;
        // Abstraction should only succeed for states with a list long enough
        assertSuccessfulAbstractionOf(dll, 4, resultState, stateToBeAbstracted, numOfAbstrLists);

      } else {
        if (topListAbstracted) {
          assertSuccessfulAbstractionOf(dll, 4, resultState, stateToBeAbstracted, numOfAbstrLists);
        } else {
          // Not abstractable, list too short
          assertNoAbstraction(resultState, stateToBeAbstracted);
        }
      }
    }

    List<SMGState> statesToTestWithAbstraction =
        statesToTestIncludingAbstractedStatesBuilder.build();
    // At least 2 abstracted states should be present so that we can test merging of abstracted with
    // abstracted. But also non-abstracted states must be present!
    assertAtLeastTwoAbstractedAndThreeNonAbstractedNestedListsIn(
        statesToTestWithAbstraction, topListAbstracted);
    return statesToTestWithAbstraction;
  }

  /**
   * Abstracts all viable states, i.e. all elements that are comparable and of min subsequent
   * length. All other states are returned as is.
   */
  private List<SMGState> abstractAbstractableStates(
      List<SMGState> statesToTest, boolean dll, int maxListLength) throws CPAException {
    ImmutableList.Builder<SMGState> statesToTestIncludingAbstractedStatesBuilder =
        ImmutableList.builder();
    for (int numOfState = 0; numOfState < statesToTest.size(); numOfState++) {
      SMGState stateToBeAbstracted = statesToTest.get(numOfState);

      SMGState resultState = abstractListsWithDefaultOptions(stateToBeAbstracted);
      statesToTestIncludingAbstractedStatesBuilder.add(resultState);

      if (numOfState + 1 >= maxListLength - 1) {
        int minimalAbstractionLen = numOfState + 1;
        if (resultState.getMemoryModel().getStackFrames().peek().getVariables().size() == 2) {
          // Elements w pointers from outside are not abstracted
          minimalAbstractionLen--;
        }
        // Abstraction should only succeed for states with a list long enough
        assertSuccessfulAbstractionOf(
            dll, minimalAbstractionLen, resultState, stateToBeAbstracted, 1);

      } else {
        // Not abstractable, list too short
        assertNoAbstraction(resultState, stateToBeAbstracted);
      }
    }

    List<SMGState> statesToTestWithAbstraction =
        statesToTestIncludingAbstractedStatesBuilder.build();
    // At least 2 abstracted states should be present so that we can test merging of abstracted with
    // abstracted. But also non-abstracted states must be present!
    assertAtLeastTwoAbstractedAndThreeNonAbstractedListsIn(statesToTestWithAbstraction);
    return statesToTestWithAbstraction;
  }

  private static void assertAtLeastTwoAbstractedAndThreeNonAbstractedListsIn(
      List<SMGState> statesToTestWithAbstraction) {
    assertThat(
            statesToTestWithAbstraction.stream()
                .filter(s -> s.getMemoryModel().getSmg().getNumberOfAbstractedLists() == 0)
                .count())
        .isAtLeast(3);
    assertThat(
            statesToTestWithAbstraction.stream()
                .filter(s -> s.getMemoryModel().getSmg().getNumberOfAbstractedLists() > 0)
                .count())
        .isAtLeast(2);
  }

  private static void assertAtLeastTwoAbstractedAndThreeNonAbstractedNestedListsIn(
      List<SMGState> statesToTestWithAbstraction, boolean topListAbstracted) {
    // Top list abstraction matters
    int minAmountOfAbstrElements = (topListAbstracted ? 1 : 0);
    assertThat(
            statesToTestWithAbstraction.stream()
                .filter(
                    s ->
                        s.getMemoryModel().getSmg().getNumberOfAbstractedLists()
                            < minAmountOfAbstrElements)
                .count())
        .isEqualTo(0);
    assertThat(
            statesToTestWithAbstraction.stream()
                .filter(
                    s ->
                        s.getMemoryModel().getSmg().getNumberOfAbstractedLists()
                            == minAmountOfAbstrElements)
                .count())
        .isAtLeast(3);
    assertThat(
            statesToTestWithAbstraction.stream()
                .filter(
                    s ->
                        s.getMemoryModel().getSmg().getNumberOfAbstractedLists()
                            > minAmountOfAbstrElements)
                .count())
        .isAtLeast(2);
  }

  private SMGState abstractListsWithDefaultOptions(SMGState stateToBeAbstracted)
      throws SMGException {
    return new SMGCPAAbstractionManager(
            stateToBeAbstracted,
            smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold(),
            new SMGCPAStatistics())
        .findAndAbstractLists();
  }

  private static void assertNoAbstraction(SMGState resultState, SMGState stateToBeAbstracted) {
    assertThat(resultState == stateToBeAbstracted).isTrue();
    assertThat(stateToBeAbstracted.getMemoryModel().getSmg().getNumberOfAbstractedLists())
        .isEqualTo(0);
    assertThat(resultState.getMemoryModel().getSmg().getNumberOfAbstractedLists()).isEqualTo(0);
  }

  private static void assertSuccessfulAbstractionOf(
      boolean dll,
      int expectedMinimalMinLength,
      SMGState abstractedState,
      SMGState stateToBeAbstracted,
      int expectedAmountOfAbstractedLists) {
    assertThat(abstractedState == stateToBeAbstracted).isFalse();
    assertThat(stateToBeAbstracted.getMemoryModel().getSmg().getNumberOfAbstractedLists())
        .isEqualTo(0);
    assertThat(abstractedState.getMemoryModel().getSmg().getNumberOfAbstractedLists())
        .isEqualTo(expectedAmountOfAbstractedLists);
    for (SMGObject abstrObj :
        abstractedState.getMemoryModel().getSmg().getAllValidAbstractedObjects()) {
      assertThat(abstrObj.getMinLength()).isAtLeast(expectedMinimalMinLength);
      if (dll) {
        assertThat(abstrObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(abstrObj).isInstanceOf(SMGDoublyLinkedListSegment.class);
      } else {
        assertThat(abstrObj).isInstanceOf(SMGSinglyLinkedListSegment.class);
        assertThat(abstrObj).isNotInstanceOf(SMGDoublyLinkedListSegment.class);
      }
    }
  }

  private void assertNotMergeable(List<SMGState> statesToTest) throws CPAException {
    for (int listLeft = 0;
        listLeft < smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold();
        listLeft++) {
      for (int listRight = listLeft + 1;
          listRight < smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold();
          listRight++) {
        Optional<MergedSMGStateAndMergeStatus> mergeResLeftRight =
            mergeOp.mergeForTests(statesToTest.get(listLeft), statesToTest.get(listRight));
        assertThat(mergeResLeftRight).isEmpty();
        Optional<MergedSMGStateAndMergeStatus> mergeResRightLeft =
            mergeOp.mergeForTests(statesToTest.get(listRight), statesToTest.get(listLeft));
        assertThat(mergeResRightLeft).isEmpty();
      }
    }
  }

  /**
   * Generates top lists of length 1 to "min abstraction + 1" of the spec given and nested lists of
   * length 1 to "min abstraction + 1" nested only below the last top list element. Nesting of the
   * return value is as follows: the list of states is nested lists ascening in size from 1 to max.
   * The list of list of states is all possible permutations of nested lists per top-list, e.g. for
   * a top list of length 2, there is 2 lists. The outermost is the length of top lists.
   *
   * @param nestedListHasStackPointerAtTheEnd true adds a pointer to the end of the nested list from
   *     the variable given. Else no stack var points to the nested directly.
   */
  private List<List<List<SMGState>>> generateListsWithSingleNestedListsFromSpec(
      IFunction<Integer, Map<String, Integer>, List<ListSpec>, ListSpec> spec,
      Map<String, Integer> variableAndPointerLocationInList,
      BiFunction<Integer, Map<String, Integer>, ListSpec> nestedListSpec,
      String nestedListStackVariable,
      boolean nestedListHasStackPointerAtTheEnd,
      int maxListLength)
      throws CPATransferException {
    ImmutableList.Builder<List<List<SMGState>>> listsByLengthBuilder = ImmutableList.builder();
    ImmutableMap<String, Integer> nestedListExternalPtr = ImmutableMap.of();
    if (nestedListHasStackPointerAtTheEnd) {
      nestedListExternalPtr = ImmutableMap.of(nestedListStackVariable, Integer.MAX_VALUE);
    }

    // Generate top-lists of length 1 to abstraction minimum + 1
    for (int topListLength = 1; topListLength <= maxListLength; topListLength++) {

      ImmutableList.Builder<List<SMGState>> listOfNestedLists = ImmutableList.builder();
      for (int walker = 0; walker < topListLength; walker++) {
        ImmutableList.Builder<SMGState> nestedListBuilder = ImmutableList.builder();
        // Generate the nested list only for the most recent top-list-element
        for (int nestedListDepth = 1; nestedListDepth <= maxListLength; nestedListDepth++) {
          List<ListSpec> nestedList = new ArrayList<>();
          for (int i = 0; i < topListLength; i++) {
            if (i == walker) {
              nestedList.add(nestedListSpec.apply(nestedListDepth, nestedListExternalPtr));
            } else {
              nestedList.add(null);
            }
          }

          nestedListBuilder.add(
              buildConcreteListWith(
                  spec.create(topListLength, variableAndPointerLocationInList, nestedList)));
        }
        listOfNestedLists.add(nestedListBuilder.build());
      }
      List<List<SMGState>> listOfListOfNestedLists = listOfNestedLists.build();
      assertThat(listOfListOfNestedLists).hasSize(topListLength);
      listsByLengthBuilder.add(listOfListOfNestedLists);
    }

    List<List<List<SMGState>>> statesToTest = listsByLengthBuilder.build();
    assertThat(statesToTest).hasSize(maxListLength);
    return statesToTest;
  }

  /** Generates lists of length 1 to "min abstraction + 1" of the spec given. */
  private List<SMGState> generateListsFromSpec(
      BiFunction<Integer, Map<String, Integer>, ListSpec> spec,
      Map<String, Integer> variableAndPointerLocationInList,
      int maxListLength)
      throws CPATransferException {
    SMGState initialState = currentState;
    ImmutableList.Builder<SMGState> statesToTestBuilder = ImmutableList.builder();

    // Generate concrete lists of length 1 to abstraction minimum + 1
    for (int listLength = 1; listLength <= maxListLength; listLength++) {
      ListSpec concreteSpec = spec.apply(listLength, variableAndPointerLocationInList);

      if (listLength == 1) {
        // Make sure that shared values are truly shared between the states!
        addSharedValuesToCurrentState();
      }

      statesToTestBuilder.add(buildConcreteListWith(concreteSpec));
    }

    List<SMGState> statesToTest = statesToTestBuilder.build();
    assertThat(statesToTest).hasSize(maxListLength);
    currentState = initialState;
    return statesToTest;
  }

  // Values lazily generated for each state anew if not for this, resulting in actual new and
  // distinct values when the same value is wanted. So we generate them once in the shared
  // state if truly shared and identical values are requested.
  // Save and reset the current state if you want to reuse it!
  private void addSharedValuesToCurrentState() {
    List<List<Value>> sharedValues = sharedValuesInListSpec;
    for (List<Value> sharedValuesInAList : sharedValues) {
      for (Value sharedValue : sharedValuesInAList) {
        currentState = currentState.copyAndAddValue(sharedValue, CNumericTypes.INT).getSMGState();
      }
    }
  }

  // TODO: add ability to define where to point to inside the nested list
  // TODO: add machine model based sizes (so that we can test 64 bits)

  /**
   * Uses the currentState SMGState initially and returns the modified state with the list inside.
   * Guarantees that the currentState in the end is equal to the initial. Always writes all values
   * to 0 before writing pointers, desired values etc. so all fields not modified are 0.
   */
  protected SMGState buildConcreteListWith(ListSpec listToBuild) throws CPATransferException {
    SMGState initialState = currentState;
    Map<String, Integer> stackVariableForPtrToListAndLocation =
        listToBuild.getStackVariableForPtrToListAndLocation();

    checkArgument(
        !stackVariableForPtrToListAndLocation.isEmpty(),
        "At least some list pointer needs to be in a stack variable");
    for (Entry<String, Integer> entry : stackVariableForPtrToListAndLocation.entrySet()) {
      checkArgument(
          entry.getValue() >= 0,
          "Specified local variable %s"
              + " that is supposed to reference a new list segment that lies"
              + " outside of the possible list with index: %s",
          entry.getKey(),
          entry.getValue());
    }

    internalBuildConcreteListWith(listToBuild);

    SMGState returnState = currentState;
    currentState = initialState;
    assertThat(returnState.copyAndPruneUnreachable().hasMemoryErrors()).isFalse();
    return returnState;
  }

  protected SMGObject[] internalBuildConcreteListWith(ListSpec listToBuild)
      throws CPATransferException {
    int listLength = listToBuild.getListLength();
    List<ListSpec> nestedLists = listToBuild.getNestedLists();
    Map<String, Integer> stackVariableForPtrToListAndLocation =
        listToBuild.getStackVariableForPtrToListAndLocation();
    BigInteger nextPtrOffset = listToBuild.getNextPointerTargetOffset();
    Optional<BigInteger> maybePrevPtrOffset = listToBuild.getPrevPointerTargetOffset();
    BigInteger sizeOfSegment = listToBuild.getSizeOfSegment();
    boolean dll = listToBuild.isDll();

    // Values that are unique to each list generated by a spec, i.e. even handing the same list to 2
    // (non-nested list) specs will yield 2 lists with distinct values!
    List<List<Value>> valuesToFill = listToBuild.getValuesToFill();
    // TODO: implement a mixed case here, most likely through adding offsets, i.e. make the second
    // list a map.
    assertThat(sharedValuesInListSpec.isEmpty() || (valuesToFill == null || valuesToFill.isEmpty()))
        .isTrue();
    if (valuesToFill == null || valuesToFill.isEmpty()) {
      valuesToFill = sharedValuesInListSpec;
    }
    BigInteger nextOffset = listToBuild.getNextOffset();
    Optional<BigInteger> maybePrevOffset = listToBuild.getPrevOffset();

    SMGObject[] listObjects = new SMGObject[listLength];

    checkArgument(
        !dll || maybePrevOffset.isPresent(),
        "You need to specify the prev pointers offset in list segments when building a DLL");
    checkArgument(
        !dll || maybePrevPtrOffset.isPresent(),
        "You need to specify the prev pointers offset towards the other segments when building a"
            + " DLL");
    checkArgument(listLength > 0, "Your list needs to be at least 1 element long");
    checkArgument(
        !dll || nextOffset.intValueExact() != maybePrevOffset.orElseThrow().intValueExact());
    checkArgument(
        !dll || nextOffset.intValueExact() < maybePrevOffset.orElseThrow().intValueExact(),
        "Conceptually, the next pointer needs to be before the prev pointer");
    checkArgument(
        sizeOfSegment.intValueExact() >= 32,
        "Each list segment needs at least space for a single pointer");
    checkArgument(sizeOfSegment.intValueExact() > nextOffset.intValueExact());
    checkArgument(sizeOfSegment.intValueExact() >= nextOffset.intValueExact() + 32);

    // Gen top list
    SMGObject prevObject = null;
    for (int numOfListSegment = 0; numOfListSegment < listLength; numOfListSegment++) {
      // Gen list segment
      SMGObject listSegment = SMGObject.of(0, sizeOfSegment, BigInteger.ZERO);
      currentState = currentState.copyAndAddObjectToHeap(listSegment);
      listObjects[numOfListSegment] = listSegment;
      // Write everything to 0
      for (int j = 0; j < sizeOfSegment.divide(pointerSizeInBits).intValue(); j++) {
        currentState =
            currentState.writeValueWithChecks(
                listSegment,
                new NumericValue(BigInteger.valueOf(j).multiply(pointerSizeInBits)),
                numericPointerSizeInBits,
                new NumericValue(BigInteger.ZERO),
                CNumericTypes.INT,
                dummyCFAEdge);
      }

      // Make next pointer 0 for current list segment if it is the last
      if (numOfListSegment == listLength - 1) {
        currentState =
            currentState.writeValueWithChecks(
                listSegment,
                new NumericValue(nextOffset),
                numericPointerSizeInBits,
                new NumericValue(0),
                CPointerType.POINTER_TO_VOID,
                dummyCFAEdge);
      }

      // Set next pointer in prev object
      if (prevObject != null) {
        checkState(numOfListSegment != 0);
        ValueAndSMGState pointerAndState =
            currentState.searchOrCreateAddress(
                listSegment, CPointerType.POINTER_TO_VOID, nextPtrOffset);
        currentState = pointerAndState.getState();
        currentState =
            currentState.writeValueWithChecks(
                prevObject,
                new NumericValue(nextOffset),
                numericPointerSizeInBits,
                pointerAndState.getValue(),
                CPointerType.POINTER_TO_VOID,
                dummyCFAEdge);
      }

      // Set prev pointers if DLL
      if (dll) {
        // Pointer to the prev list segment
        Value prevPointer;
        if (numOfListSegment == 0) {
          prevPointer = new NumericValue(BigInteger.ZERO);
        } else {
          ValueAndSMGState pointerAndState =
              currentState.searchOrCreateAddress(
                  prevObject, CPointerType.POINTER_TO_VOID, maybePrevPtrOffset.orElseThrow());
          prevPointer = pointerAndState.getValue();
          currentState = pointerAndState.getState();
        }
        currentState =
            currentState.writeValueWithChecks(
                listSegment,
                new NumericValue(maybePrevOffset.orElseThrow()),
                numericPointerSizeInBits,
                prevPointer,
                CPointerType.POINTER_TO_VOID,
                dummyCFAEdge);
      }

      prevObject = listSegment;
    }
    assertThat(currentState.hasMemoryErrors()).isFalse();

    // Write wanted values
    if (valuesToFill != null && !valuesToFill.isEmpty()) {
      for (int elementIndex = 0; elementIndex < listLength; elementIndex++) {
        // If the check below fails: is the padding for shared values to low?
        checkArgument(
            valuesToFill.size() >= listLength,
            "Specifying values to set must happen for none or all list segments");
        List<Value> valuesToFillInThisElement = valuesToFill.get(elementIndex);
        SMGObject objToWriteTo = listObjects[elementIndex];
        BigInteger currentHeadOffset = BigInteger.ZERO;

        for (Value valueToWrite : valuesToFillInThisElement) {
          // First find correct offset
          while (currentHeadOffset.equals(nextOffset)
              || (dll && currentHeadOffset.equals(maybePrevOffset.orElseThrow()))) {
            currentHeadOffset = currentHeadOffset.add(pointerSizeInBits);
          }

          // TODO: add type to input
          checkState(
              sizeOfSegment.intValueExact() > currentHeadOffset.intValueExact(),
              "Too many values to write to list of this size");
          if (valueToWrite != null) {
            // If this fails, implement proper types
            checkState(
                machineModel.getSizeofInBits(CNumericTypes.INT) == 32
                    && pointerSizeInBits.intValueExact() == 32);
            checkState(!currentHeadOffset.equals(nextOffset));
            checkState(!dll || !currentHeadOffset.equals(maybePrevOffset.orElseThrow()));
            currentState =
                currentState.writeValueWithChecks(
                    objToWriteTo,
                    new NumericValue(currentHeadOffset),
                    new NumericValue(pointerSizeInBits),
                    valueToWrite,
                    CNumericTypes.INT,
                    dummyCFAEdge);
          }
          currentHeadOffset = currentHeadOffset.add(pointerSizeInBits);
        }
      }
    }
    assertThat(currentState.hasMemoryErrors()).isFalse();

    // Write pointers to the given stack vars
    for (Entry<String, Integer> varAndLoc : stackVariableForPtrToListAndLocation.entrySet()) {
      String varName = varAndLoc.getKey();
      int varIndex = varAndLoc.getValue();

      if (!currentState.isLocalOrGlobalVariablePresent(varName)) {
        SMGObjectAndSMGState stackObjAndState =
            currentState.copyAndAddStackObject(numericPointerSizeInBits);
        currentState = stackObjAndState.getState();
        SMGObject dummyStackObject = stackObjAndState.getSMGObject();
        currentState =
            currentState.copyAndAddLocalVariable(
                dummyStackObject, varName, CPointerType.POINTER_TO_VOID);
      }

      SMGObject objToPointTowards;
      if (varIndex >= listObjects.length) {
        objToPointTowards = listObjects[listObjects.length - 1];
      } else {
        objToPointTowards = listObjects[varIndex];
      }
      // TODO: add offset specifier
      ValueAndSMGState pointerAndState =
          currentState.searchOrCreateAddress(
              objToPointTowards, CPointerType.POINTER_TO_VOID, BigInteger.ZERO);
      Value pointer = pointerAndState.getValue();
      currentState = pointerAndState.getState();

      currentState =
          currentState.writeToStackOrGlobalVariable(
              varName,
              new NumericValue(BigInteger.ZERO),
              numericPointerSizeInBits,
              pointer,
              CPointerType.POINTER_TO_VOID,
              dummyCFAEdge);
    }
    assertThat(currentState.copyAndPruneUnreachable().hasMemoryErrors()).isFalse();

    // Add nested lists as specified
    if (!nestedLists.isEmpty()) {
      checkArgument(
          nextOffset.intValueExact() - 32 == 0 || nextOffset.intValueExact() == 0,
          "Nested lists are currently only supported if there is exactly space for 1 value (size 32"
              + " bit) in the list segments");
      BigInteger headOffset = BigInteger.ZERO;
      if (nextOffset.intValueExact() == 0) {
        checkState(nextOffset.intValueExact() == headOffset.intValueExact());
        headOffset = BigInteger.valueOf(32);
      }
      if (dll) {
        if (nextOffset.intValueExact() == 0
            && maybePrevOffset.orElseThrow().intValueExact() == 32) {
          headOffset = BigInteger.valueOf(64);
        } else {
          checkState(
              headOffset == BigInteger.ZERO
                  && nextOffset.intValueExact() == 32
                  && maybePrevOffset.orElseThrow().intValueExact() == 64);
        }
      }

      checkState(sizeOfSegment.intValueExact() > headOffset.intValueExact());
      for (int elementIndex = 0; elementIndex < listLength; elementIndex++) {
        ListSpec nestedListSpec = nestedLists.get(elementIndex);
        SMGObject objToAddNestedListTo = listObjects[elementIndex];

        if (nestedListSpec == null) {
          // no nested list here, make sure we point to 0 or some expected value and continue
          SMGValue value =
              currentState
                  .readSMGValue(objToAddNestedListTo, headOffset, pointerSizeInBits)
                  .getSMGValue();

          Value expectedValue = new NumericValue(BigInteger.ZERO);
          if (valuesToFill != null
              && valuesToFill.size() > elementIndex
              && !valuesToFill.get(elementIndex).isEmpty()) {
            expectedValue = valuesToFill.get(elementIndex).getFirst();
          }
          Optional<SMGValue> expectedSMGValue =
              currentState.getMemoryModel().getSMGValueFromValue(expectedValue);
          assertThat(expectedSMGValue).isPresent();
          assertThat(value).isEqualTo(expectedSMGValue.orElseThrow());
          continue;
        }

        // TODO: add ability to define where to point to inside the nested list
        SMGObject[] nestedListObjs = internalBuildConcreteListWith(nestedListSpec);

        // TODO: add ability to set pointer offset
        ValueAndSMGState pointerAndState =
            currentState.searchOrCreateAddress(
                nestedListObjs[0], CPointerType.POINTER_TO_VOID, BigInteger.ZERO);
        Value pointer = pointerAndState.getValue();
        currentState = pointerAndState.getState();

        currentState =
            currentState.writeValueWithChecks(
                objToAddNestedListTo,
                new NumericValue(headOffset),
                new NumericValue(pointerSizeInBits),
                pointer,
                CPointerType.POINTER_TO_VOID,
                dummyCFAEdge);

        assertThat(currentState.copyAndPruneUnreachable().hasMemoryErrors()).isFalse();
      }
    }

    return listObjects;
  }

  // Specifies how a list is supposed to be built
  public static final class ListSpec {
    private boolean dll;
    private BigInteger sizeOfSegment;

    private BigInteger nextPointerTargetOffset;
    private Optional<BigInteger> prevPointerTargetOffset;
    private int listLength;

    private BigInteger nextOffset;
    private Optional<BigInteger> prevOffset;

    // Creates the stack vars if not present. If the location (int) exceeds the list length, the
    // last list element is used!
    private Map<String, Integer> stackVariableForPtrToListAndLocation;

    // First list is list element to give values to, second is values per segment.
    // The values will be generated when the list is generated, i.e. 2 lists will have distinct
    // elements, even when the same one is put in the list!
    // All types are INT
    private List<List<Value>> valuesToFill;

    // Put on first free space besides next and prev
    private List<ListSpec> nestedLists;

    private ListSpec(
        Map<String, Integer> pStackVariableForPtrToListAndLocation,
        boolean pDll,
        BigInteger pSizeOfSegment,
        int pListLength,
        BigInteger pNextPointerTargetOffset,
        Optional<BigInteger> pPrevPointerTargetOffset,
        List<ListSpec> pNestedList,
        List<List<Value>> pValuesToFill,
        BigInteger pNextOffset,
        Optional<BigInteger> pPrevOffset) {
      checkArgument(!dll || !pPrevPointerTargetOffset.isEmpty());
      dll = pDll;
      sizeOfSegment = pSizeOfSegment;
      nextPointerTargetOffset = pNextPointerTargetOffset;
      prevPointerTargetOffset = pPrevPointerTargetOffset;
      stackVariableForPtrToListAndLocation = pStackVariableForPtrToListAndLocation;
      nestedLists = pNestedList;
      listLength = pListLength;
      valuesToFill = pValuesToFill;
      nextOffset = pNextOffset;
      prevOffset = pPrevOffset;
    }

    @SuppressWarnings("unused")
    private void replaceNestedListSpecs(List<ListSpec> newNestedLists) {
      nestedLists = newNestedLists;
    }

    public static ListSpec getSllWithNoValues(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(32),
          length,
          BigInteger.ZERO,
          Optional.empty(),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.ZERO,
          Optional.empty());
    }

    public static ListSpec getDllWithNoValues(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          true,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.ZERO,
          Optional.of(BigInteger.valueOf(32)));
    }

    public static ListSpec getSllWithSingleZeroValueAfterNfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.empty(),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.ZERO,
          Optional.empty());
    }

    public static ListSpec getDllWithSingleZeroValueAfterNfoAndPfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          true,
          BigInteger.valueOf(96),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.ZERO,
          Optional.of(BigInteger.valueOf(32)));
    }

    public static ListSpec getSllWithSingleZeroValueBeforeNfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.empty(),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.valueOf(32),
          Optional.empty());
    }

    public static ListSpec getDllWithSingleZeroValueBeforeNfoAndPfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          true,
          BigInteger.valueOf(96),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.valueOf(32),
          Optional.of(BigInteger.valueOf(64)));
    }

    public static ListSpec getSllWithSingleValuesGivenAfterNfo(
        int length,
        Map<String, Integer> stackVariableForPtrToListAndLocation,
        List<List<Value>> valuesToFill) {
      checkArgument(valuesToFill != null && valuesToFill.size() == length);
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.empty(),
          ImmutableList.of(),
          valuesToFill,
          BigInteger.ZERO,
          Optional.empty());
    }

    public static ListSpec getDllWithSingleValuesGivenAfterNfoAndPfo(
        int length,
        Map<String, Integer> stackVariableForPtrToListAndLocation,
        List<List<Value>> valuesToFill) {
      checkArgument(valuesToFill != null && valuesToFill.size() == length);
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          true,
          BigInteger.valueOf(96),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          valuesToFill,
          BigInteger.ZERO,
          Optional.of(BigInteger.valueOf(32)));
    }

    public static ListSpec getSllWithSingleValuesGivenBeforeNfo(
        int length,
        Map<String, Integer> stackVariableForPtrToListAndLocation,
        List<List<Value>> valuesToFill) {
      checkArgument(valuesToFill != null && valuesToFill.size() == length);
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.empty(),
          ImmutableList.of(),
          valuesToFill,
          BigInteger.valueOf(32),
          Optional.empty());
    }

    public static ListSpec getDllWithSingleValuesGivenBeforeNfoAndPfo(
        int length,
        Map<String, Integer> stackVariableForPtrToListAndLocation,
        List<List<Value>> valuesToFill) {
      checkArgument(valuesToFill != null && valuesToFill.size() == length);
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          true,
          BigInteger.valueOf(96),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          valuesToFill,
          BigInteger.valueOf(32),
          Optional.of(BigInteger.valueOf(64)));
    }

    public static ListSpec getSllWithSingleIdenticalNondetIntValueAfterNfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      if (sharedValuesInListSpec.isEmpty()) {
        ImmutableList.Builder<List<Value>> nondetValues = ImmutableList.builder();
        SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
        Value nondetValue =
            ConstantSymbolicExpression.of(factory.newIdentifier(null), CNumericTypes.INT);
        // This is called with the smallest list, length 1, first. So we need to pad this
        for (int i = 0; i < length + 5; i++) {
          nondetValues.add(ImmutableList.of(nondetValue));
        }
        sharedValuesInListSpec = nondetValues.build();
      }
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.ZERO,
          Optional.empty());
    }

    public static ListSpec getSllWithSingleIdenticalNondetIntValueAfterNfo(
        int length,
        Map<String, Integer> stackVariableForPtrToListAndLocation,
        List<ListSpec> nestedListsSpec) {
      // This spec does not support nested lists. The parameter is only meant to support the
      // interface!
      assertThat(nestedListsSpec).isEmpty();
      if (sharedValuesInListSpec.isEmpty()) {
        ImmutableList.Builder<List<Value>> nondetValues = ImmutableList.builder();
        SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
        Value nondetValue =
            ConstantSymbolicExpression.of(factory.newIdentifier(null), CNumericTypes.INT);
        // This is called with the smallest list, length 1, first. So we need to pad this
        for (int i = 0; i < length + 5; i++) {
          nondetValues.add(ImmutableList.of(nondetValue));
        }
        sharedValuesInListSpec = nondetValues.build();
      }
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.ZERO,
          Optional.empty());
    }

    public static ListSpec getDllWithSingleIdenticalNondetIntValueAfterNfoAndPfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      if (sharedValuesInListSpec.isEmpty()) {
        ImmutableList.Builder<List<Value>> nondetValues = ImmutableList.builder();
        SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
        Value nondetValue =
            ConstantSymbolicExpression.of(factory.newIdentifier(null), CNumericTypes.INT);
        // This is called with the smallest list, length 1, first. So we need to pad this
        for (int i = 0; i < length + 5; i++) {
          nondetValues.add(ImmutableList.of(nondetValue));
        }
        sharedValuesInListSpec = nondetValues.build();
      }
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          true,
          BigInteger.valueOf(96),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.ZERO,
          Optional.of(BigInteger.valueOf(32)));
    }

    public static ListSpec getSllWithSingleIdenticalNondetIntValueBeforeNfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      if (sharedValuesInListSpec.isEmpty()) {
        ImmutableList.Builder<List<Value>> nondetValues = ImmutableList.builder();
        SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
        Value nondetValue =
            ConstantSymbolicExpression.of(factory.newIdentifier(null), CNumericTypes.INT);
        // This is called with the smallest list, length 1, first. So we need to pad this
        for (int i = 0; i < length + 5; i++) {
          nondetValues.add(ImmutableList.of(nondetValue));
        }
        sharedValuesInListSpec = nondetValues.build();
      }
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.valueOf(32),
          Optional.empty());
    }

    public static ListSpec getSllWithSingleIdenticalNondetIntValueBeforeNfoAndPfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      if (sharedValuesInListSpec.isEmpty()) {
        ImmutableList.Builder<List<Value>> nondetValues = ImmutableList.builder();
        SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
        Value nondetValue =
            ConstantSymbolicExpression.of(factory.newIdentifier(null), CNumericTypes.INT);
        // This is called with the smallest list, length 1, first. So we need to pad this
        for (int i = 0; i < length + 5; i++) {
          nondetValues.add(ImmutableList.of(nondetValue));
        }
        sharedValuesInListSpec = nondetValues.build();
      }
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.empty(),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.valueOf(32),
          Optional.empty());
    }

    public static ListSpec getDllWithSingleIdenticalNondetIntValueBeforeNfoAndPfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      if (sharedValuesInListSpec.isEmpty()) {
        ImmutableList.Builder<List<Value>> nondetValues = ImmutableList.builder();
        SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
        Value nondetValue =
            ConstantSymbolicExpression.of(factory.newIdentifier(null), CNumericTypes.INT);
        // This is called with the smallest list, length 1, first. So we need to pad this
        for (int i = 0; i < length + 5; i++) {
          nondetValues.add(ImmutableList.of(nondetValue));
        }
        sharedValuesInListSpec = nondetValues.build();
      }
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          true,
          BigInteger.valueOf(96),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          ImmutableList.of(),
          BigInteger.valueOf(32),
          Optional.of(BigInteger.valueOf(64)));
    }

    public static ListSpec getSllWithSingleDistinctNondetIntValueBeforeNfoAndPfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      ImmutableList.Builder<List<Value>> nondetValues = ImmutableList.builder();
      SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
      for (int i = 0; i < length; i++) {
        nondetValues.add(
            ImmutableList.of(
                ConstantSymbolicExpression.of(factory.newIdentifier(null), CNumericTypes.INT)));
      }
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.empty(),
          ImmutableList.of(),
          nondetValues.build(),
          BigInteger.valueOf(32),
          Optional.empty());
    }

    public static ListSpec getDllWithSingleDistinctNondetIntValueBeforeNfoAndPfo(
        int length, Map<String, Integer> stackVariableForPtrToListAndLocation) {
      ImmutableList.Builder<List<Value>> nondetValues = ImmutableList.builder();
      SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
      for (int i = 0; i < length; i++) {
        nondetValues.add(
            ImmutableList.of(
                ConstantSymbolicExpression.of(factory.newIdentifier(null), CNumericTypes.INT)));
      }
      return new ListSpec(
          stackVariableForPtrToListAndLocation,
          true,
          BigInteger.valueOf(96),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          ImmutableList.of(),
          nondetValues.build(),
          BigInteger.valueOf(32),
          Optional.of(BigInteger.valueOf(64)));
    }

    public static ListSpec getBareDllWithNestedListBeforeNextAndPrevOf(
        int length,
        Map<String, Integer> stackVariableForPtrToTopListAndLocation,
        List<ListSpec> nestedListsSpec) {
      return new ListSpec(
          stackVariableForPtrToTopListAndLocation,
          true,
          BigInteger.valueOf(96),
          length,
          BigInteger.ZERO,
          Optional.of(BigInteger.ZERO),
          nestedListsSpec,
          ImmutableList.of(),
          BigInteger.valueOf(32),
          Optional.of(BigInteger.valueOf(64)));
    }

    public static ListSpec getBareSllWithNestedListBeforeNextOf(
        int length,
        Map<String, Integer> stackVariableForPtrToTopListAndLocation,
        List<ListSpec> nestedListsSpec) {
      return new ListSpec(
          stackVariableForPtrToTopListAndLocation,
          false,
          BigInteger.valueOf(64),
          length,
          BigInteger.ZERO,
          Optional.empty(),
          nestedListsSpec,
          ImmutableList.of(),
          BigInteger.valueOf(32),
          Optional.empty());
    }

    public List<List<Value>> getValuesToFill() {
      return valuesToFill;
    }

    public boolean isDll() {
      return dll;
    }

    public BigInteger getSizeOfSegment() {
      return sizeOfSegment;
    }

    public BigInteger getNextPointerTargetOffset() {
      return nextPointerTargetOffset;
    }

    public Optional<BigInteger> getPrevPointerTargetOffset() {
      return prevPointerTargetOffset;
    }

    public int getListLength() {
      return listLength;
    }

    public Map<String, Integer> getStackVariableForPtrToListAndLocation() {
      return stackVariableForPtrToListAndLocation;
    }

    public List<ListSpec> getNestedLists() {
      return nestedLists;
    }

    public BigInteger getNextOffset() {
      return nextOffset;
    }

    public Optional<BigInteger> getPrevOffset() {
      return prevOffset;
    }
  }
}
