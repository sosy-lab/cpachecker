/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.ForwardTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A transition system with a global transition relation, modeling all transitions in CFA in one
 * boolean formula. It basically is the disjunction of all transitions between block heads in the
 * CFA. A program counter variable is conjoined to all block transitions to encode the block start
 * and end locations. Also provides formulas for the initial condition and the safety property.
 */
public class TransitionSystem {

  private static final String PROGRAM_COUNTER_VARIABLE_NAME = "__CPAchecker_pc";
  private static final CType PROGRAM_COUNTER_TYPE = CNumericTypes.UNSIGNED_INT;

  private static final int STANDARD_UNPRIMED_SSA = 1;
  private static final int PC_PRIMED_SSA = 2; // Only need two different indices for pc

  private static final SSAMap MAP_WITH_DEFAULT_UNPRIMED =
      SSAMap.emptySSAMap().withDefault(STANDARD_UNPRIMED_SSA);
  private static final SSAMap MAP_WITH_PRIMED_PC = SSAMap.emptySSAMap().withDefault(PC_PRIMED_SSA);

  private final PDROptions optionsCollection;
  private final BooleanFormula transitionRelation;
  private final BooleanFormula initialCondition;
  private final BooleanFormula safetyProperty;
  private final Map<Integer, CFANode> idToLocation;
  private final Set<CFANode> targetLocs;
  private final PathFormula unprimedContext;
  private final PathFormula primedContext;

  /**
   * Creates a new TransitionSystem for the given CFA.
   *
   * @param pOptions The collection containing the configuration options that are relevant for the
   *     transition system.
   * @param pCFA The CFA that this transition relation is based on.
   * @param pForwardTransition The component that computes the single block transitions.
   * @param pFmgr The used formula manager.
   * @param pPfmgr The used path formula manager.
   * @param pMainEntry The initial location.
   * @throws CPAException If the analysis creating the blocks encounters an exception.
   * @throws InterruptedException If the computation of the blocks is interrupted.
   * @throws InvalidConfigurationException If the configuration file is invalid.
   */
  public TransitionSystem(
      PDROptions pOptions,
      CFA pCFA,
      ForwardTransition pForwardTransition,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      CFANode pMainEntry)
      throws CPAException, InterruptedException, InvalidConfigurationException {

    this.optionsCollection = Objects.requireNonNull(pOptions);
    Objects.requireNonNull(pCFA);
    Objects.requireNonNull(pFmgr);
    Objects.requireNonNull(pPfmgr);
    Objects.requireNonNull(pForwardTransition);
    Objects.requireNonNull(pMainEntry);
    BooleanFormulaManagerView bfmgr = pFmgr.getBooleanFormulaManager();
    BitvectorFormulaManagerView bvfmgr = pFmgr.getBitvectorFormulaManager();
    this.idToLocation = new TreeMap<>();
    this.initialCondition = makeProgramcounterFormula(getID(pMainEntry), bvfmgr, pCFA);

    this.targetLocs = new HashSet<>();
    PathFormula transWithPrimedContext =
        createTransitionRelation(
            pForwardTransition, pFmgr, bfmgr, bvfmgr, pPfmgr, pCFA, pMainEntry);
    this.transitionRelation = transWithPrimedContext.getFormula();
    this.safetyProperty = createSafetyProperty(bfmgr, bvfmgr, pCFA);

    // Create path formula containing primed indices.
    SSAMap primed = transWithPrimedContext.getSsa();
    this.primedContext =
        new PathFormula(
            bfmgr.makeTrue(),
            primed,
            transWithPrimedContext.getPointerTargetSet(),
            transWithPrimedContext.getLength());

    // Create path formula containing unprimed indices.
    SSAMapBuilder unprimedSSAs = SSAMap.emptySSAMap().builder();
    for (String var : primed.allVariables()) {
      CType type = primed.getType(var);
      unprimedSSAs.setIndex(var, type, STANDARD_UNPRIMED_SSA);
    }
    this.unprimedContext = pPfmgr.makeNewPathFormula(primedContext, unprimedSSAs.build());
  }

  /**
   * Collects all blocks in the CFA, adds the program counter to the block formula and creates a big
   * disjunction of those formulas. Takes care of adjusting the ssa indices for primed and unprimed
   * variables in different block formulas.
   */
  private PathFormula createTransitionRelation(
      ForwardTransition pForwardTransition,
      FormulaManagerView pFmgr,
      BooleanFormulaManagerView pBfmgr,
      BitvectorFormulaManagerView pBvfmgr,
      PathFormulaManager pPfmgr,
      CFA pCFA,
      CFANode pMainEntry)
      throws CPAException, InterruptedException {

    Collection<Block> exploredBlocks = getForwardReachableBlocks(pMainEntry, pForwardTransition);

    if (optionsCollection.shouldRemoveRedundantTransitions()) {
      exploredBlocks = getBackwardUnreachableBlocks(exploredBlocks, pBfmgr);
    }

    // if there are no blocks, the transition formula is "false".
    if (exploredBlocks.isEmpty()) {
      return pPfmgr.makeEmptyPathFormula().updateFormula(pBfmgr.makeFalse());
    }

    Iterator<Block> exploredBlockIterator = exploredBlocks.iterator();
    Block first = exploredBlockIterator.next();
    PathFormula transitionRelation =
        withCorrectionTermsAndPC(first, pFmgr, pBfmgr, pBvfmgr, pPfmgr, pCFA);

    while (exploredBlockIterator.hasNext()) {
      Block next = exploredBlockIterator.next();
      PathFormula adjusted = withCorrectionTermsAndPC(next, pFmgr, pBfmgr, pBvfmgr, pPfmgr, pCFA);

      /*
       * This takes care of disjoining the formulas themselves, adding correction terms for
       * primed variables and merging primed ssa maps and pointer target sets.
       */
      transitionRelation = pPfmgr.makeOr(transitionRelation, adjusted);
    }

    return transitionRelation;
  }

  /**
   * Performs a DFS and collects all blocks that are forward reachable from the provided starting
   * location.
   */
  private Collection<Block> getForwardReachableBlocks(
      CFANode pStartPoint, ForwardTransition pForwardTransition)
      throws CPAException, InterruptedException {
    Collection<Block> exploredBlocks = new LinkedList<>();
    Deque<Block> blockTraversalStack = new LinkedList<>();

    for (Block block : pForwardTransition.getBlocksFrom(pStartPoint)) {
      saveIfSuccessorIsErrorLocation(block);
      blockTraversalStack.push(block);
      exploredBlocks.add(block);
    }

    while (!blockTraversalStack.isEmpty()) {
      Block currentBlock = blockTraversalStack.pop();

      // Continue recursively with new successors of current block.
      CFANode currentBlockSuccessorLocation = currentBlock.getSuccessorLocation();
      for (Block block : pForwardTransition.getBlocksFrom(currentBlockSuccessorLocation)) {
        if (!isBlockContainedIn(block, exploredBlocks)) {
          saveIfSuccessorIsErrorLocation(block);
          blockTraversalStack.push(block);
          exploredBlocks.add(block);
        }
      }
    }
    return exploredBlocks;
  }

  /** Gets all blocks that are backwards reachable from error locations. */
  private List<Block> getBackwardUnreachableBlocks(
      Collection<Block> pExploredBlocks, BooleanFormulaManagerView pBfmgr) {
    List<Block> relevantBlocks = new LinkedList<>();
    Deque<Block> backwardsTraversalStack = new LinkedList<>();

    // Find error predecessor blocks
    for (Block block : pExploredBlocks) {
      if (targetLocs.contains(block.getSuccessorLocation())) {
        backwardsTraversalStack.push(block);
        relevantBlocks.add(block);
      }
    }

    /*
     *  When the transition from an error predecessor location to its error location is a
     *  disjunction, a separate block for each disjunctive part may be found.
     *  Only keep the one block with the full transition formula.
     */
    List<Block> relevantBlockCopy = new ArrayList<>(relevantBlocks);
    for (int i = 0; i < relevantBlockCopy.size() - 1; ++i) {
      Block block1 = relevantBlockCopy.get(i);

      for (int j = i + 1; j < relevantBlockCopy.size(); ++j) {
        Block block2 = relevantBlockCopy.get(j);

        if (block1.getPredecessorLocation().equals(block2.getPredecessorLocation())) {
          Set<BooleanFormula> block1DisjunctionArgs =
              pBfmgr.toDisjunctionArgs(block1.getFormula(), false);
          Set<BooleanFormula> block2DisjunctionArgs =
              pBfmgr.toDisjunctionArgs(block2.getFormula(), false);

          if (block1DisjunctionArgs.containsAll(block2DisjunctionArgs)) {
            relevantBlocks.remove(block2);
            backwardsTraversalStack.remove(block2);
          } else if (block2DisjunctionArgs.containsAll(block1DisjunctionArgs)) {
            relevantBlocks.remove(block1);
            backwardsTraversalStack.remove(block1);
          }
        }
      }
    }

    // Traverse blocks backward from error pred blocks and save all found blocks.
    while (!backwardsTraversalStack.isEmpty()) {
      Block current = backwardsTraversalStack.pop();

      // add/push predecessor blocks of current
      for (Block other : pExploredBlocks) {
        if (isPredecessorBlockOf(other, current) && !isBlockContainedIn(other, relevantBlocks)) {
          relevantBlocks.add(other);
          backwardsTraversalStack.push(other);
        }
      }
    }

    return relevantBlocks;
  }

  /**
   * Takes the block formula from the block and checks if the unprimed ssa index for all variables
   * is 1. If not, conjoins (var_1 = var_unprimedSSA).
   *
   * <p>Adds formulas for the program counter before and after the block transition. Updates the ssa
   * map for the primed context with the program counter.
   *
   * @return A path formula with the above mentioned adjustments to the block transition formula.
   *     The ssa map contains the indices for primed variables including the pc. The pointer target
   *     set is the same as before.
   */
  private PathFormula withCorrectionTermsAndPC(
      Block pBlock,
      FormulaManagerView pFmgr,
      BooleanFormulaManagerView pBfmgr,
      BitvectorFormulaManagerView pBvfmgr,
      PathFormulaManager pPfmgr,
      CFA pCFA) {

    BooleanFormula extendedBlockFormula = pBlock.getFormula();
    PathFormula oldPrimedContext = pBlock.getPrimedContext();
    SSAMap oldPrimedSSAMap = oldPrimedContext.getSsa();
    PathFormula newUnprimedContext =
        pPfmgr.makeNewPathFormula(oldPrimedContext, MAP_WITH_DEFAULT_UNPRIMED);

    // Look at each variable "v" in block formula and get lowest ssa index "low" for it.
    // If low != 1, conjoin correction term (v_low = v_1)
    for (Map.Entry<String, Integer> entry :
        getLowestIndexForVariableOccurences(pBlock, pFmgr, pBfmgr, pPfmgr).entrySet()) {
      String varName = entry.getKey();
      Integer lowestIndex = entry.getValue();

      if (lowestIndex != STANDARD_UNPRIMED_SSA) {
        PathFormula oldUnprimedContext =
            pPfmgr.makeNewPathFormula(
                oldPrimedContext, SSAMap.emptySSAMap().withDefault(lowestIndex));
        CType type = oldPrimedSSAMap.getType(varName);
        BitvectorFormula varWithOldSSA =
            (BitvectorFormula)
                pPfmgr.makeFormulaForVariable(oldUnprimedContext, varName, type, false);
        BitvectorFormula varWithNewSSA =
            (BitvectorFormula)
                pPfmgr.makeFormulaForVariable(newUnprimedContext, varName, type, false);

        BooleanFormula correctionterm = pBvfmgr.equal(varWithOldSSA, varWithNewSSA);
        extendedBlockFormula = pBfmgr.and(extendedBlockFormula, correctionterm);
      }
    }

    // Add program counter.
    int predID = getID(pBlock.getPredecessorLocation());
    int succID = getID(pBlock.getSuccessorLocation());
    BooleanFormula pcBefore =
        pFmgr.instantiate(
            makeProgramcounterFormula(predID, pBvfmgr, pCFA), MAP_WITH_DEFAULT_UNPRIMED);
    BooleanFormula pcAfter =
        pFmgr.instantiate(makeProgramcounterFormula(succID, pBvfmgr, pCFA), MAP_WITH_PRIMED_PC);
    extendedBlockFormula = pBfmgr.and(pcBefore, extendedBlockFormula, pcAfter);

    // Update primed ssa map with pc.
    SSAMap primedSSAMapWithPC =
        oldPrimedSSAMap
            .builder()
            .setIndex(PROGRAM_COUNTER_VARIABLE_NAME, PROGRAM_COUNTER_TYPE, PC_PRIMED_SSA)
            .build();

    PathFormula withAdjustedFormulaAndPrimedContext =
        pPfmgr
            .makeNewPathFormula(oldPrimedContext, primedSSAMapWithPC)
            .updateFormula(extendedBlockFormula);

    return withAdjustedFormulaAndPrimedContext;
  }

  private Map<String, Integer> getLowestIndexForVariableOccurences(
      Block pBlock,
      FormulaManagerView pFmgr,
      BooleanFormulaManagerView pBfmgr,
      PathFormulaManager pPfmgr) {

    Map<String, Integer> lowestIndexForVariable = new HashMap<>();
    BooleanFormula formula = pBlock.getFormula();
    if (pBfmgr.isTrue(formula) || pBfmgr.isFalse(formula)) {
      return lowestIndexForVariable;
    }

    Set<String> instantiatedVariableNames = pFmgr.extractVariableNames(formula);
    SSAMap ssa = pBlock.getPrimedContext().getSsa();

    for (String varName : ssa.allVariables()) {
      CType type = ssa.getType(varName);
      BitvectorFormula variable =
          (BitvectorFormula)
              pPfmgr.makeFormulaForVariable(pBlock.getPrimedContext(), varName, type, false);

      // Try instantiating variable with indices from 1 to max until match found.
      int currentIndex = 1;
      while (!lowestIndexForVariable.containsKey(varName)
          && (currentIndex <= ssa.getIndex(varName))) {
        variable = pFmgr.instantiate(variable, SSAMap.emptySSAMap().withDefault(currentIndex));
        String nameWithCurrentIndex =
            Iterables.getOnlyElement(pFmgr.extractVariableNames(variable));

        if (instantiatedVariableNames.contains(nameWithCurrentIndex)) {
          lowestIndexForVariable.put(varName, currentIndex);
        }
        currentIndex++;
      }
    }

    return lowestIndexForVariable;
  }

  private static boolean isBlockContainedIn(Block pBlock, Collection<Block> pCollection) {
    return pCollection.stream().anyMatch(pBlock::equalsIgnoreReachedSet);
  }

  private static boolean isPredecessorBlockOf(Block pPred, Block pSucc) {
    return pPred.getSuccessorLocation().equals(pSucc.getPredecessorLocation());
  }

  private void saveIfSuccessorIsErrorLocation(Block pBlock) {
    if (AbstractStates.asIterable(pBlock.getSuccessor()).anyMatch(AbstractStates.IS_TARGET_STATE)) {
      targetLocs.add(pBlock.getSuccessorLocation());
    }
  }

  /** Returns the formula (pc=pLocationNumber). */
  private BooleanFormula makeProgramcounterFormula(
      int pLocationNumber, BitvectorFormulaManagerView pBvfmgr, CFA pCFA) {
    int bitLength =
        pCFA.getMachineModel().getSizeof(PROGRAM_COUNTER_TYPE)
            * pCFA.getMachineModel().getSizeofCharInBits();
    BitvectorFormula pc = pBvfmgr.makeVariable(bitLength, PROGRAM_COUNTER_VARIABLE_NAME);
    BitvectorFormula value = pBvfmgr.makeBitvector(bitLength, pLocationNumber);
    return pBvfmgr.equal(pc, value);
  }

  /** SafetyProperty = (pc != l_E1) & (pc != l_E2) ... for all error locations. */
  private BooleanFormula createSafetyProperty(
      BooleanFormulaManagerView pBfmgr, BitvectorFormulaManagerView pBvfmgr, CFA pCFA) {
    BooleanFormula safetyProperty = pBfmgr.makeTrue();
    for (CFANode errorLocation : targetLocs) {
      int id = getID(errorLocation);
      safetyProperty =
          pBfmgr.and(safetyProperty, pBfmgr.not(makeProgramcounterFormula(id, pBvfmgr, pCFA)));
    }
    return safetyProperty;
  }

  /**
   * Returns a unique identifier for each node that is used as value for the program counter. Also
   * caches this mapping.
   */
  private int getID(CFANode pLocation) {
    int id = pLocation.getNodeNumber();
    idToLocation.putIfAbsent(id, pLocation);
    return id;
  }

  /**
   * Returns a set of all names for variables occurring in this transition system. The program
   * counter is excluded.
   *
   * <p>If the dedicated name for the program counter is needed, use {@link #programCounterName()}.
   */
  public Set<String> allVariableNames() {
    Set<String> withoutPC = new HashSet<>(primedContext.getSsa().allVariables());
    withoutPC.remove(PROGRAM_COUNTER_VARIABLE_NAME);
    return withoutPC;
  }

  /**
   * Returns the global transition relation as a single boolean formula.
   *
   * @return A boolean formula describing the transition relation.
   */
  public BooleanFormula getTransitionRelationFormula() {
    return transitionRelation;
  }

  /**
   * Returns the location with the specified identifier used in the transition relation.
   *
   * @param pID The identifier of the wanted location.
   * @return An Optional containing the CFANode with this id, or an empty Optional if no such
   *     CFANode exists.
   */
  public Optional<CFANode> getNodeForID(int pID) {
    return Optional.ofNullable(idToLocation.get(pID));
  }

  /**
   * Returns the used String representation of the program counter variable.
   *
   * @return The String representation of the program counter variable.
   */
  public String programCounterName() {
    return PROGRAM_COUNTER_VARIABLE_NAME;
  }

  /**
   * Returns the set of all target location for this transition system.
   *
   * @return A set of all target locations.
   */
  public Set<CFANode> getTargetLocations() {
    return targetLocs;
  }

  /**
   * Returns a path formula containing the ssa map for the primed variables, i.e. the variables
   * after one transition step.
   *
   * @return A path formula with the primed ssa indices.
   */
  public PathFormula getPrimedContext() {
    return primedContext;
  }

  /**
   * Returns a path formula containing the ssa map for the unprimed variables, i.e. the variables at
   * the start of a transition step.
   *
   * @return A path formula with the unprimed ssa indices.
   */
  public PathFormula getUnprimedContext() {
    return unprimedContext;
  }

  /**
   * Returns a formula describing the initial condition (pc = start location) for this transition
   * system.
   *
   * @return The initial condition (pc = start location).
   */
  public BooleanFormula getInitialCondition() {
    return initialCondition;
  }

  /**
   * Returns a formula describing the safety property for this transition system. This formula
   * encodes that the program counter is never at a target location: (pc != error location 1) & (pc
   * != error location 2) & ... for all error locations.
   *
   * @return The safety property of this transition system.
   */
  public BooleanFormula getSafetyProperty() {
    return safetyProperty;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Initial condition : \n").append(initialCondition);
    sb.append("\nSafety property : \n").append(safetyProperty);
    sb.append("\nTransition formula : \n").append(transitionRelation);
    return sb.toString();
  }
}
