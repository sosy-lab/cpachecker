// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.cpachecker.util.expressions.ExpressionTrees.FUNCTION_DELIMITER;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Verify;
import java.io.Serializable;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.cpa.arg.Splitable;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public abstract sealed class TPAAbstractState
    implements AbstractState, Partitionable, Serializable, Splitable {

  /**
   * The path formula for the path from the last abstraction node to this node. it is set to true on
   * a new abstraction location and updated with a new non-abstraction location
   */
  private PathFormula pathFormula;
  /** The abstraction which is updated only on abstraction locations */
  private AbstractionFormula abstractionFormula;
  /** How often each abstraction location was visited on the path to the current state. */
  private final transient PersistentMap<CFANode, Integer> abstractionLocations;

  /** TODO: Link to previous AbstractionState?   */
  private final TPAAbstractState.AbstractionState previousAbstractionState;




  // TODO: Constructors: One for the initial state with no previous state, the other is for following states
  private TPAAbstractState(
      PathFormula pf, AbstractionFormula a, PersistentMap<CFANode, Integer> pAbstractionLocations) {
    pathFormula = pf;
    abstractionFormula = a;
    abstractionLocations = pAbstractionLocations;
    previousAbstractionState = null;
  }
  private TPAAbstractState(
      PathFormula pf,
      AbstractionFormula a,
      PersistentMap<CFANode, Integer> pAbstractionLocations,
      TPAAbstractState pPreviousAbstractionState) {
    pathFormula = pf;
    abstractionFormula = a;
    abstractionLocations = pAbstractionLocations;
    previousAbstractionState = (TPAAbstractState.AbstractionState) pPreviousAbstractionState;
  }

  /**
   * Public static constructors for parent and child class
   */
  /**
   * Create AbstractionState object
   * @param pF
   * @param pA
   * @param pAbstractionLocations
   * @return Simply return a new object of class AbstractionState
   */
  public static TPAAbstractState mkAbstractionState(
      PathFormula pF,
      AbstractionFormula pA,
      PersistentMap<CFANode, Integer> pAbstractionLocations) {
    return new TPAAbstractState.AbstractionState(pF, pA, pAbstractionLocations);
  }
  /**
   * Create AbstractionState object
   * @param pF
   * @param pA
   * @param pAbstractionLocations
   * @param pPreviousAbstractionState
   * @return Simply return a new object of class AbstractionState
   */
  public static TPAAbstractState mkAbstractionState(
      PathFormula pF,
      AbstractionFormula pA,
      PersistentMap<CFANode, Integer> pAbstractionLocations,
      TPAAbstractState pPreviousAbstractionState) {
    return new TPAAbstractState.AbstractionState(pF, pA, pAbstractionLocations, pPreviousAbstractionState);
  }
  /**
   * Constructor for child class NonAbstractionState
   * @param pF
   * @param pA
   * @param pAbstractionLocations
   * @return
   */
  static TPAAbstractState mkNonAbstractionState(
      PathFormula pF,
      AbstractionFormula pA,
      PersistentMap<CFANode, Integer> pAbstractionLocations) {
    return new TPAAbstractState.NonAbstractionState(pF, pA, pAbstractionLocations);
  }
  /**
   * Create a new NonAbstractionState with input new PathFormula. AbstractionFormula and AbstractionLocationsOnPath stay the same.
   * pseudo PathFormula setters
   * @param pF
   * @param oldState
   * @return new NonAbstractionState with input PathFormula
   */
  public static TPAAbstractState mkNonAbstractionStateWithNewPathFormula(
      PathFormula pF,
      TPAAbstractState oldState) {
    return new TPAAbstractState.NonAbstractionState(
        pF,
        oldState.getAbstractionFormula(),
        oldState.getAbstractionLocationsOnPath());
  }
  /**
   * Create a new NonAbstractionState with input new PathFormula. AbstractionFormula and AbstractionLocationsOnPath stay the same.
   * pseudo PathFormula setters
   * @param pF
   * @param oldState
   * @param pPreviousAbstractionState
   * @return new NonAbstractionState with input PathFormula
   */
  public static TPAAbstractState mkNonAbstractionStateWithNewPathFormula(
      PathFormula pF,
      TPAAbstractState oldState,
      TPAAbstractState pPreviousAbstractionState) {
    return new TPAAbstractState.NonAbstractionState(
        pF,
        oldState.getAbstractionFormula(),
        oldState.getAbstractionLocationsOnPath(),
        pPreviousAbstractionState);
  }



  // Getters and Setters
  public AbstractionFormula getAbstractionFormula() {
    return abstractionFormula;
  }
  /**
   * TODO: Check PredicateAbstractionState for warning note of this methos
   * @param pAbstractionFormula
   */
  public void setAbstraction(AbstractionFormula pAbstractionFormula) {
    if (isAbstractionState()) {
      abstractionFormula = checkNotNull(pAbstractionFormula);
    } else {
      throw new UnsupportedOperationException(
          "Changing abstraction formula is only supported for abstraction elements");
    }
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public PersistentMap<CFANode, Integer> getAbstractionLocationsOnPath() {
    return abstractionLocations;
  }
  public TPAAbstractState getPreviousAbstractionState() {
    return previousAbstractionState;
  }
  /**
   * Get the state that should be set as merged
   * @return
   */
  TPAAbstractState getMergedInto() {
    throw new UnsupportedOperationException("Assuming wrong TPAAbstractStates were merged!");
  }
  /**
   * Mark this state as merged with another state.
   *
   * @param pMergedInto the state that should be set as merged
   */
  void setMergedInto(TPAAbstractState pMergedInto) {
    throw new UnsupportedOperationException("Merging wrong TPAAbstractStates!");
  }
  /**
   * TODO: Fix docu if necessary
   * Determine if it's abstracted or not
   * @return true if state is AbstractionState else NonAbstractionSate
   */
  public abstract boolean isAbstractionState();


  /** Class methods */
  public static boolean containsAbstractionState(AbstractState state) {
    return AbstractStates.extractStateByType(state, TPAAbstractState.class)
        .isAbstractionState();
  }

  public static TPAAbstractState getPredicateState(AbstractState pState) {
    return checkNotNull(extractStateByType(pState, TPAAbstractState.class));
  }

  public static BooleanFormula getBlockFormula(TPAAbstractState pState) {
    checkArgument(pState.isAbstractionState());
    return pState.getAbstractionFormula().getBlockFormula().getFormula();
  }

  @Override
  public AbstractState forkWithReplacements(Collection<AbstractState> pReplacementStates) {
    for (AbstractState state : pReplacementStates) {
      if (state instanceof TPAAbstractState) {
        return state;
      }
    }
    return this;
  }


  /**
   * NonAbstractionState child class
    */
  private static final class NonAbstractionState extends TPAAbstractState {

    /** The abstract state this element was merged into. Used for fast coverage checks. */
    private transient TPAAbstractState mergedInto = null;

    // 2 constructors corresponding to 2 constructors of TPAAbstractState parent class
    private NonAbstractionState(
        PathFormula pF,
        AbstractionFormula pA,
        PersistentMap<CFANode, Integer> pAbstractionLocations) {
      super(pF, pA, pAbstractionLocations);
    }
    private NonAbstractionState(
        PathFormula pF,
        AbstractionFormula pA,
        PersistentMap<CFANode, Integer> pAbstractionLocations,
        TPAAbstractState pPreviousAbstractState) {
      super(pF, pA, pAbstractionLocations, pPreviousAbstractState);
    }

    // Getters and Setters
    @Override
    public @Nullable Object getPartitionKey() {
      return null;
    }
    @Override
    public boolean isAbstractionState() {
      return false;
    }
    @Override
    TPAAbstractState getMergedInto() {
      return mergedInto;
    }
    @Override
    void setMergedInto(TPAAbstractState pMergedInto) {
      Preconditions.checkNotNull(pMergedInto);
      mergedInto = pMergedInto;
    }
    @Override
    public String toString() {
      return "Abstraction location of NonAbstractState: false";
    }
  }


  /**
   * AbstractionState child class
   */
  private static final class AbstractionState extends TPAAbstractState
      implements Graphable, FormulaReportingState, ExpressionTreeReportingState {
    /** The abstract state this element was merged into. Used for fast coverage checks. */
    private transient TPAAbstractState mergedInto = null;

    // Constructors
    private AbstractionState(
        PathFormula pf,
        AbstractionFormula pA,
        PersistentMap<CFANode, Integer> pAbstractionLocations) {
      super(pf, pA, pAbstractionLocations);
      // Check whether the pathFormula of an abstraction element is just "true".
      // partialOrder relies on this for optimization.
      // Preconditions.checkArgument(bfmgr.isTrue(pf.getFormula()));
      // Check uncommented because we may pre-initialize the path formula
      // with an invariant.
      // This is no problem for the partial order because the invariant
      // is always the same when the location is the same.
    }
    private AbstractionState(
        PathFormula pf,
        AbstractionFormula pA,
        PersistentMap<CFANode, Integer> pAbstractionLocations,
        TPAAbstractState pPreviousAbstractState) {
      super(pf, pA, pAbstractionLocations, pPreviousAbstractState);
    }

    // Getters and Setters
    @Override
    public TPAAbstractState getMergedInto() {
      return mergedInto;
    }
    @Override
    public void setMergedInto(TPAAbstractState pMergedInto) {
      mergedInto = pMergedInto;
    }
    @Override
    public boolean isAbstractionState() {
      return true;
    }
    @Override
    public String toString() {
      return "Abstraction location: true, Abstraction: " + super.abstractionFormula;
    }
    @Override
    public boolean shouldBeHighlighted() {
      return true;
    }
    @Override
    public String toDOTLabel() {
      return super.abstractionFormula.toString();
    }
    @Override
    public @Nullable Object getPartitionKey() {
      if (super.abstractionFormula.isFalse()) {
        // put unreachable states in a separate partition to avoid merging
        // them with any reachable states
        return Boolean.FALSE;
      } else {
        return null;
      }
    }
    @Override
    public AbstractState forkWithReplacements(Collection<AbstractState> pReplacementStates) {
      return null;
    }


    // Getter for Approximation
    @Override
    public ExpressionTree<Object> getFormulaApproximationInputProgramInScopeVariables(
        FunctionEntryNode pFunctionScope,
        CFANode pLocation,
        AstCfaRelation pAstCfaRelation,
        boolean useOldKeywordForVariables)
        throws InterruptedException,
               ReportingMethodNotImplementedException,
               TranslationToExpressionTreeFailedException {
      return super.abstractionFormula.asExpressionTree(
          name ->
              (!name.contains(FUNCTION_DELIMITER)
                  || name.startsWith(pLocation.getFunctionName() + FUNCTION_DELIMITER))
                  && pAstCfaRelation
                  .getVariablesAndParametersInScope(pLocation)
                  .anyMatch(
                      var ->
                          // For local variables
                          (pLocation.getFunctionName() + FUNCTION_DELIMITER + var.getName())
                              .equals(name)
                              // For global variables
                              || var.getName().equals(name))
                  && !name.contains("__CPAchecker_"),
          name -> useOldKeywordForVariables ? "\\old(" + name + ")" : name);
    }
    @Override
    public ExpressionTree<Object> getFormulaApproximationFunctionReturnVariableOnly(
        FunctionEntryNode pFunctionScope, AIdExpression pFunctionReturnVariable)
        throws InterruptedException, TranslationToExpressionTreeFailedException {
      Verify.verify(pFunctionScope.getExitNode().isPresent());
      FunctionExitNode functionExitNode = pFunctionScope.getExitNode().orElseThrow();
      String smtNameReturnVariable = "__retval__";
      return super.abstractionFormula.asExpressionTree(
          name ->
              name.startsWith(functionExitNode.getFunctionName() + FUNCTION_DELIMITER)
                  && Splitter.on(FUNCTION_DELIMITER)
                  .splitToList(name)
                  .get(1)
                  .equals(smtNameReturnVariable),
          name -> name.equals(smtNameReturnVariable) ? pFunctionReturnVariable.getName() : name);
    }
    @Override
    public ExpressionTree<Object> getFormulaApproximationAllVariablesInFunctionScope(
        FunctionEntryNode pFunctionScope, CFANode pLocation)
        throws InterruptedException, TranslationToExpressionTreeFailedException {
      return super.abstractionFormula.asExpressionTree(pLocation);
    }
    @Override
    public BooleanFormula getFormulaApproximation(FormulaManagerView pManager) {
      return super.abstractionFormula.asFormulaFromOtherSolver(pManager);
    }
    @Override
    public BooleanFormula getScopedFormulaApproximation(
        final FormulaManagerView pManager, final FunctionEntryNode pFunctionScope) {
      try {
        return pManager.renameFreeVariablesAndUFs(
            pManager.filterLiterals(
                super.abstractionFormula.asFormulaFromOtherSolver(pManager),
                literal -> {
                  for (String name : pManager.extractVariableNames(literal)) {
                    if (name.contains("::") && !name.startsWith(pFunctionScope.getFunctionName())) {
                      return false;
                    }
                  }
                  return true;
                }),
            name -> {
              int separatorIndex = name.indexOf("::");
              if (separatorIndex >= 0) {
                return name.substring(separatorIndex + 2);
              } else {
                return name;
              }
            });
      } catch (InterruptedException e) {
        return pManager.getBooleanFormulaManager().makeTrue();
      }
    }
  }
}
