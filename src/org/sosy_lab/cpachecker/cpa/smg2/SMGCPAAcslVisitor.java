// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation.simplifyAssumption;
import static org.sosy_lab.cpachecker.cpa.smg2.SMGTransferRelation.representsBoolean;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCanAccessPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicPredicateDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSet;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslProgramLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslQuantifiedPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSeparateMemoryConjunctionPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibArrayConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBitVectorConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFloatingPointConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTermTuple;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRoundingModeConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermAssignmentCfaStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclarationTuple;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibInvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibNotRecurringTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRecurringTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibSymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator.BooleanAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;

/*
 * Abstraction predicate 'pred_sll' for a Singly-Linked-List (SLL) of type 'sll' defined as:
 *
 * struct sll {
 *   struct sll *next;
 * };
 *
 * With the following C code creating the list with the predicate (loop invariant) 'pred_sll':
 *
 * struct sll* create(void) {
 *   struct sll *sll = alloc_and_zero();
 *   struct sll *now = sll;
 *   while(random()) {
 *     now->next = alloc_and_zero();
 *     now = now->next;
 *   }
 *   return sll;
 * }
 *
 * The invariant is called with 'pred_sll(sll, now)'.
 *
 * Predicate in definitions:
 * pred_sll(sll * start, sll * end):
 *  start != 0 && start->next == 0 && start == end
 *    && \\canAccess(start) && \\canAccess(end)
 *  || start != 0 && start->next != start && start->next != 0
 *    && \\canAccess(start) && \\canAccess(end) && \\canAccess(start->next)
 *    && (start)@(end) && (start->next)@(start)
 *    && pred_sll(start->next, end)
 *
 * TODO: replace @ with separated and move recursive calls inside
 * Idea: we get the function call and its arguments first. We process the arguments
 * (none are known in the beginning) and remember their definitions. This allows us to find recursive calls later (i.e. we process the first function call, remember it, when we process it again, we know its recursive. This should be done as trace, to respect multiple possible recursive calls)
 *
 * TODO: rework text below now that we don't create new objects in the memory-model:
 * We then process the body;
 * The body is an ITE (ternary) expression; we check whether the conditions can be fulfilled on the current state.
 * It can, since size is ?.
 * Case split:
 * Case1:  We go into if; in this case we find "start != 0 && start->next == 0 && start == end".
 * We first find the && s (which are binary and therefore nested!);
 * I think it would be best to start with start != 0, as this establishes that there is a valid element start.
 * It does not matter whether "start->next == 0" or
 * "start == end" is next, both assume that there is a list of size 1.
 * For "start == end", we don't have to do anything in this case.
 * "start->next == 0" just writes (assumes) this info to be true.
 *
 * Case2: we visit the else case; start != 0 && start->next != start && start->next != 0
 *  && pred_sll(start->next, end, size - 1)
 * start != 0; same as above, establishes that there is a element start of the type given.
 * start->next != start; the next element is not this element. We don't have to do anything.
 * start->next != 0; the next element is not 0. We don't have to do anything.
 * pred_sll(start->next, end, size - 1); this is where the magic happens,
 * we execute the recursive call, get back a state with a list and then need to add the
 * start->next relation, i.e. we take the start list element we created and add the pointer of
 * the returned linked list start element (due to the first argument of
 * the function call being start->next, we know that we need to assign to that,
 * and the argument name is start, hence the returned pointer to assign is the returned start,
 * hence current(start)->next = returned(start))
 */
/**
 * SMG-CPA visitor for ACSL based predicates, e.g. in witnesses.
 *
 * <p>We start with the current {@link SMGState} of the analyses and confirm that all predicates
 * hold. The only thing we add is potentially information about the abstraction via the
 * recursiveness. We return an empty list if we find that a predicate does not hold, else the
 * potentially updated (or old) state.
 *
 * <p>TODO: maybe we don't even need a list, then we could switch to an optional.
 */
public class SMGCPAAcslVisitor extends AAstNodeVisitor<Set<SMGState>, CPATransferException> {

  // Are we in a negated case or not
  private final boolean truthAssumption;

  private final SMGState initialState;

  // For example definitions for functions/predicates that are called. These definitions (e.g.
  // AcslLogicPredicateDefinition) have needed information like the function bodies.
  private final ImmutableSet<AcslLogicDefinition> logicDefinitions;

  private final SMGTransferRelation callingTransfer;
  private final SMGCPAExpressionEvaluator evaluator;
  private final LogManagerWithoutDuplicates logger;
  private final SMGOptions options;

  // TODO: we use this edge in all methods that require an edge for now. Is that a good idea? We
  // sometimes build error info from the CFAEdge in SMG2.
  private final CFAEdge originCfaEdge;

  private SMGCPAAcslVisitor(
      ImmutableSet<AcslLogicDefinition> pDefinitions,
      SMGState pInitialState,
      boolean pTruthAssumption,
      SMGOptions pOptions,
      LogManagerWithoutDuplicates pLogger,
      SMGCPAExpressionEvaluator pEvaluator,
      SMGTransferRelation pCallingTransfer,
      CFAEdge pCfaEdge) {
    logicDefinitions = checkNotNull(pDefinitions);
    initialState = checkNotNull(pInitialState);
    logger = checkNotNull(pLogger);
    options = checkNotNull(pOptions);
    truthAssumption = pTruthAssumption;
    evaluator = pEvaluator;
    callingTransfer = pCallingTransfer;
    originCfaEdge = pCfaEdge;
  }

  /**
   * Checks whether it is allowed to access the memory of {@link SMGObject} {@code
   * memoryObjAccessed} from [accessOffsetInBits,accessOffsetInBits + accessSizeInBits).
   */
  private BooleanAndSMGState checkCanAccessMemory(
      Value accessOffsetInBits, Value accessSizeInBits, SMGObject memoryObjAccessed, SMGState state)
      throws CPATransferException {
    checkNotNull(memoryObjAccessed);
    checkNotNull(accessOffsetInBits);
    checkNotNull(state);
    checkArgument(!(memoryObjAccessed instanceof SMGSinglyLinkedListSegment));
    return evaluator.checkMemoryAccess(
        accessOffsetInBits, accessSizeInBits, memoryObjAccessed, state, originCfaEdge);
  }

  /**
   * Entry function to validate a predicate from a witness on the {@link SMGState} of this visitor.
   *
   * @return {@link Optional#empty()} for rejection of the invariant on the given state and a
   *     non-empty {@link Optional} with a potentially updated {@link SMGState} for accepted
   *     invariants on the given state.
   */
  public static Optional<SMGState> validate(
      final AcslPredicate invariantToValidate,
      final SMGState stateToValidateOn,
      ImmutableSet<AcslLogicDefinition> pDefinitions,
      SMGOptions pOptions,
      LogManagerWithoutDuplicates pLogger,
      SMGCPAExpressionEvaluator pEvaluator,
      SMGTransferRelation pCallingTransfer,
      CFAEdge pCfaEdge)
      throws CPATransferException {

    verify(!stateToValidateOn.hasMemoryErrors());
    // We currently only check whether canAccess() statements are correct, so we don't need
    // preprocessing for now
    // AcslPredicate preprocessedInvariant = preprocessInvariant(invariantToValidate);
    // TODO: problem: we read things before we allow it to set up ACSL stack frames!
    // TODO: Marian says we don't need to check that. I am unsure.
    // SMGState preprocessedState = preprocessState(stateToValidateOn);
    // verify(!preprocessedState.hasMemoryErrors());

    SMGCPAAcslVisitor visitor =
        new SMGCPAAcslVisitor(
            pDefinitions,
            stateToValidateOn,
            pOptions,
            pLogger,
            pEvaluator,
            pCallingTransfer,
            pCfaEdge);

    Set<SMGState> validatedStates = invariantToValidate.accept(visitor);

    // Check the resulting states for errors (=rejected witness) and sanity check
    checkNotNull(validatedStates);
    for (SMGState validatedState : validatedStates) {
      verify(
          validatedState.getStackFrameTopFunctionDefinition()
              == stateToValidateOn.getStackFrameTopFunctionDefinition());
      if (validatedState.hasMemoryErrors()) {
        return Optional.empty();
      }
    }

    // TODO: for now we return the initial state, as we reject invariants that don't hold in all
    // cases and do not change the state (besides materializing the list). Should we add information
    // from the invariant, for example abstract lists based on invariant information, we need to
    // return the updated state instead. But we also need to validate the memory again, such that it
    // matches the initial state!
    return Optional.of(stateToValidateOn);
  }

  /**
   * Preprocess the state to validate. Invalidates all memory without changing it, so that
   * canAccess() can validate it again.
   */
  @SuppressWarnings("unused")
  private static SMGState copyAndInvalidateAllMemoryInState(SMGState pStateToValidateOn) {
    return pStateToValidateOn.copyAndReplaceMemoryModel(
        pStateToValidateOn
            .getMemoryModel()
            .copyWithNewSMG(
                pStateToValidateOn
                    .getMemoryModel()
                    .getSmg()
                    .copyAndInvalidateObjects(
                        pStateToValidateOn.getMemoryModel().getSmg().getObjects())));
  }

  /**
   * Constructor for a new {@link SMGCPAAcslVisitor}. Only to be used for initial visitation. If you
   * need to modify the visitor, use the copy methods of this class, e.g. {@link
   * #copyWithNewState(SMGState)}.
   */
  private SMGCPAAcslVisitor(
      ImmutableSet<AcslLogicDefinition> pDefinitions,
      SMGState pInitialState,
      SMGOptions pOptions,
      LogManagerWithoutDuplicates pLogger,
      SMGCPAExpressionEvaluator pEvaluator,
      SMGTransferRelation pCallingTransfer,
      CFAEdge pCfaEdge) {
    logicDefinitions = checkNotNull(pDefinitions);
    initialState = checkNotNull(pInitialState);
    logger = checkNotNull(pLogger);
    options = checkNotNull(pOptions);
    truthAssumption = true;
    evaluator = pEvaluator;
    callingTransfer = pCallingTransfer;
    originCfaEdge = pCfaEdge;
  }

  private SMGCPAAcslVisitor copyWithNewState(SMGState newInitialState) {
    return new SMGCPAAcslVisitor(
        logicDefinitions,
        newInitialState,
        truthAssumption,
        options,
        logger,
        evaluator,
        callingTransfer,
        originCfaEdge);
  }

  @Override
  protected Set<SMGState> visit(AFunctionCallExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected Set<SMGState> visit(AInitializerExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected Set<SMGState> visit(AFunctionCallStatement stmt) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected Set<SMGState> visit(AFunctionCallAssignmentStatement stmt) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected Set<SMGState> visit(AExpressionStatement stmt) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected Set<SMGState> visit(AExpressionAssignmentStatement stmt) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected Set<SMGState> visit(AReturnStatement stmt) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected Set<SMGState> visit(AFunctionDeclaration decl) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected Set<SMGState> visit(AParameterDeclaration decl) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  protected Set<SMGState> visit(AVariableDeclaration decl) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AArraySubscriptExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AIdExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(ABinaryExpression exp) throws CPATransferException {
    // Check that the assumptions posed are correct (reject non-logical operators)
    if (exp instanceof CBinaryExpression cExpr && cExpr.getOperator().isLogicalOperator()) {
      return handleAssumptionWithSimplification(initialState, cExpr);
    }

    throw new UnsupportedOperationException("Handling for " + exp + " currently not implemented");
  }

  @Override
  public Set<SMGState> visit(ACastExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(ACharLiteralExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AFloatLiteralExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AIntegerLiteralExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AStringLiteralExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AUnaryExpression exp) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslBinaryPredicate exp) throws CPATransferException {
    /*
     * This typically encodes relations, e.g. in linked-lists. Example:
     *
     * start != 0 && start->next != start && start->next != 0 && pred_sll()
     *
     * pred_sll() is a recursive function call,
     * while the others encode linked-list behavior that has to hold.
     * Note: since this is binary, the example above would be in a nested fashion, hence we flatten it first.
     */

    // We only need && currently, and don't expect others.
    if (!exp.getOperator().equals(AcslBinaryPredicateOperator.AND)) {
      throw new IllegalStateException(
          "Unhandled binary operator: " + exp.getOperator() + " in ACSL predicate");
    }

    ImmutableSet.Builder<SMGState> res = ImmutableSet.builder();
    res.addAll(exp.getOperand1().accept(this));
    res.addAll(exp.getOperand2().accept(this));

    return res.build();

    /*
    List<AcslPredicate> flattenedArgumentForOperator =
        flattenNestedBinaryPredicatesByGivenOperator(
            exp, (AcslBinaryPredicateOperator) exp.getOperator());

    // Add handling if it fails.
    checkState(
        flattenedArgumentForOperator.stream().noneMatch(p -> p instanceof AcslBinaryPredicate));

    // TODO: simplify into one once we know this works
    List<AcslPredicateTerm> allTermsInPredicates =
        flattenedArgumentForOperator.stream()
            .filter(p -> p instanceof AcslPredicateTerm)
            .map(p -> (AcslPredicateTerm) p)
            .collect(ImmutableList.toImmutableList());
    List<CExpression> allCExprsInTerms =
        allTermsInPredicates.stream()
            .filter(t -> t.getTerm() instanceof AcslCExpressionTerm)
            .map(t -> ((AcslCExpressionTerm) t.getTerm()).getCExpression())
            .collect(ImmutableList.toImmutableList());
    // We only expect at max 1 recursive function call (that is processed last)
    checkState(allTermsInPredicates.size() - 1 == allCExprsInTerms.size());
    List<CBinaryExpression> allCBinExprsInCExprs =
        allCExprsInTerms.stream()
            .filter(t -> t instanceof CBinaryExpression)
            .map(t -> (CBinaryExpression) t)
            .collect(ImmutableList.toImmutableList());
    checkState(allCExprsInTerms.size() == allCBinExprsInCExprs.size());

    // TODO: order and process

    // We want to process start != 0 or start == 0 predicates first! If there is none,
    // we need to add both.
    List<CBinaryExpression> allCBinExprsComparingIdExprAgainstLiterals =
        allCBinExprsInCExprs.stream()
            .filter(
                t ->
                    (t.getOperand1() instanceof CIntegerLiteralExpression
                            && t.getOperand2() instanceof CIdExpression)
                        || (t.getOperand2() instanceof CIntegerLiteralExpression
                            && t.getOperand1() instanceof CIdExpression))
            .collect(ImmutableList.toImmutableList());
    // Lets assume one of the 2 is always present for now (fix one it fails)
    checkState(allCBinExprsComparingIdExprAgainstLiterals.size() == 1);
    CBinaryExpression isItZeroOrNotExpr = allCBinExprsComparingIdExprAgainstLiterals.getFirst();
    BinaryOperator isItZeroOrNotExprOp = isItZeroOrNotExpr.getOperator();
    CIdExpression listElement;
    // Confirm that we are comparing to 0
    if (isItZeroOrNotExpr.getOperand1() instanceof CIntegerLiteralExpression litLeftExpr) {
      checkState(0 == litLeftExpr.getValue().intValueExact());
      listElement = (CIdExpression) isItZeroOrNotExpr.getOperand2();
    } else {
      checkState(
          0
              == ((CIntegerLiteralExpression) isItZeroOrNotExpr.getOperand2())
                  .getValue()
                  .intValueExact());
      listElement = (CIdExpression) isItZeroOrNotExpr.getOperand1();
    }

    boolean listElementIsZero;
    if (isItZeroOrNotExprOp.equals(BinaryOperator.EQUALS)) {
      listElementIsZero = true;
    } else {
      checkState(isItZeroOrNotExprOp.equals(BinaryOperator.NOT_EQUALS));
      listElementIsZero = false;
    }
    // TODO: Not zero means write new element of that type at CIDExpression
    // TODO: zero means write 0 at CIDExpression

    // Other predicates (except recursive function calls), e.g. start->next != start
    // (filter out already processed before executing)

    // Start with something like start == end, start != end
    List<CBinaryExpression> allCBinExprsComparingIdExprAgainstIdExprs =
        allCBinExprsInCExprs.stream()
            .filter(
                t ->
                    (t.getOperand1() instanceof CIdExpression
                        && t.getOperand2() instanceof CIdExpression))
            .collect(ImmutableList.toImmutableList());
    checkState(allCBinExprsComparingIdExprAgainstIdExprs.size() == 1);
    CBinaryExpression idCmpId = allCBinExprsComparingIdExprAgainstIdExprs.getFirst();

    // Recursive function call (there may be one, or none)
    Optional<AcslPredicate> recFunCall =
        flattenedArgumentForOperator.stream()
            .filter(p -> p instanceof AcslFunctionCallPredicate)
            .findFirst();
    if (recFunCall.isPresent()) {}
    */
    // Return built things

  }

  @SuppressWarnings("unused")
  private List<AcslPredicate> flattenNestedBinaryPredicatesByGivenOperator(
      AcslBinaryPredicate exp, AcslBinaryPredicateOperator opToFlatten) {
    if (opToFlatten.equals(exp.getOperator())) {
      ImmutableList.Builder<AcslPredicate> flattenedListBuilder = ImmutableList.builder();
      if (exp.getOperand1() instanceof AcslBinaryPredicate op1
          && op1.getOperator().equals(opToFlatten)) {
        List<AcslPredicate> nestedFlattened1 =
            flattenNestedBinaryPredicatesByGivenOperator(op1, opToFlatten);
        checkState(nestedFlattened1.size() >= 2);
        flattenedListBuilder.addAll(nestedFlattened1);
      } else {
        flattenedListBuilder.add(exp.getOperand1());
      }
      if (exp.getOperand2() instanceof AcslBinaryPredicate op2
          && op2.getOperator().equals(opToFlatten)) {
        List<AcslPredicate> nestedFlattened2 =
            flattenNestedBinaryPredicatesByGivenOperator(op2, opToFlatten);
        checkState(nestedFlattened2.size() >= 2);
        flattenedListBuilder.addAll(nestedFlattened2);
      } else {
        flattenedListBuilder.add(exp.getOperand2());
      }
      return flattenedListBuilder.build();
    }

    return ImmutableList.of();
  }

  @Override
  public Set<SMGState> visit(AcslLogicFunctionDefinition pAcslLogicFunctionDefinition)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslLogicPredicateDefinition pAcslLogicPredicateDefinition)
      throws CPATransferException {
    throw new UnsupportedOperationException("Definitions can not be processed on their own");
  }

  @Override
  public Set<SMGState> visit(AcslUnaryPredicate pAcslUnaryPredicate) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslIdPredicate pAcslIdPredicate) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslBinaryTermPredicate pAcslBinaryTermPredicate)
      throws CPATransferException {
    // E.g. comparisons of C terms like 'start->next != 0', or ITE conditions like 'size == 1'
    Optional<CExpression> cOperand1 = asCExpression(pAcslBinaryTermPredicate.getOperand1());
    Optional<CExpression> cOperand2 = asCExpression(pAcslBinaryTermPredicate.getOperand2());
    if (cOperand1.isPresent() && cOperand2.isPresent()) {
      CBinaryExpression cComparison =
          new CBinaryExpressionBuilder(initialState.getMachineModel(), logger)
              .buildBinaryExpressionUnchecked(
                  cOperand1.orElseThrow(),
                  cOperand2.orElseThrow(),
                  toCBinaryOperator(pAcslBinaryTermPredicate.getOperator()));
      return cComparison.accept(this);
    }

    throw new UnsupportedOperationException(
        "AcslBinaryTermPredicate failed to be resolved for " + pAcslBinaryTermPredicate);
    /*
    AcslBinaryTermExpressionOperator op = pAcslBinaryTermPredicate.getOperator();
    Set<SMGState> left = pAcslBinaryTermPredicate.getOperand1().accept(this);
    Set<SMGState> right = pAcslBinaryTermPredicate.getOperand2().accept(this);

    if (op == AcslBinaryTermExpressionOperator.EQUALS) {

    } else if (op == AcslBinaryTermExpressionOperator.NOT_EQUALS) {

    } else {
      throw new UnsupportedOperationException(
          "AcslBinaryTermPredicate failed to be resolved for " + pAcslBinaryTermPredicate);
    }

    if (truthAssumption) {
      // Negate result

    }
    return */
  }

  private static BinaryOperator toCBinaryOperator(AcslBinaryTermPredicateOperator pOperator) {
    return switch (pOperator) {
      case EQUALS -> BinaryOperator.EQUALS;
      case NOT_EQUALS -> BinaryOperator.NOT_EQUALS;
      case LESS_EQUAL -> BinaryOperator.LESS_EQUAL;
      case GREATER_EQUAL -> BinaryOperator.GREATER_EQUAL;
      case LESS_THAN -> BinaryOperator.LESS_THAN;
      case GREATER_THAN -> BinaryOperator.GREATER_THAN;
    };
  }

  @Override
  public Set<SMGState> visit(AcslOldPredicate pAcslOldPredicate) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslBooleanLiteralPredicate pAcslBooleanLiteralPredicate)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslTernaryPredicate pAcslTernaryPredicate)
      throws CPATransferException {
    /*
     * ITE; for example the split by size in a linked-list witness:
     * size == 1 ? start->next == 0 && start == end
     *   : start != 0 && start->next != start && start->next != 0 && ...
     */

    ImmutableSet.Builder<SMGState> res = ImmutableSet.builder();
    /*
        AcslPredicate condition = pAcslTernaryPredicate.getCondition();
        // 'this' uses the current truth-assumption
        Set<SMGState> evaluatedConditionTrue = condition.accept(this);
        if (!evaluatedConditionTrue.isEmpty()) {
          for (SMGState conditionTrueState : evaluatedConditionTrue) {
            res.addAll(pAcslTernaryPredicate.getResultIfTrue().accept(new SMGCPAAcslVisitor(conditionTrueState, truthAssumption, options, logger)));
          }
        }

        // Negated truth-assumption to get the "not" case
        Set<SMGState> evaluatedConditionFalse = condition.accept(new SMGCPAAcslVisitor(initialState, !truthAssumption, options, logger));
        if (!evaluatedConditionFalse.isEmpty()) {
          for (SMGState conditionFalseState : evaluatedConditionFalse) {
            res.addAll(pAcslTernaryPredicate.getResultIfFalse().accept(new SMGCPAAcslVisitor(conditionFalseState, truthAssumption, options, logger)));
          }
        }
    */
    // We evaluate all paths for now since we lack a full ACSL impl
    res.addAll(pAcslTernaryPredicate.getResultIfTrue().accept(this));
    res.addAll(pAcslTernaryPredicate.getResultIfFalse().accept(this));

    return res.build();
  }

  @Override
  public Set<SMGState> visit(AcslValidPredicate pAcslValidPredicate) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslQuantifiedPredicate pQuantifiedPredicate)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslMemoryLocationSet pMemoryLocationSet) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  // AcslPredicateApplicationPredicate are function-calls for AcslLogicFunctionDefinitions which
  // have a AcslLogicPredicateDefinition
  @Override
  public Set<SMGState> visit(AcslPredicateApplicationPredicate pAcslPredicateApplicationPredicate)
      throws CPATransferException {
    /*
     * Either entry point of a predicate defined in a witness,
     * e.g. function-call: pred_sll(sll, now, size), with 'sll' and 'now' being variables in the
     * actual C program, or, after that, a (recursive) function call,
     * e.g. pred_sll(start->next, end, size - 1), with start and end being variables defined by the functions parameters (see below).
     *
     * The recursive function 'pred_sll' can for example be used to describe a linked-list in a
     * witness:
     *
     * pred_sll(sll * start, sll * end, int size):
     *   size == 1 ? start->next == 0 && start == end
     *               && canAccess(start) && canAccess(start->next) && canAccess(end)
     *   : start != 0 && start->next != start && start->next != 0
     *     && canAccess(start) && canAccess(start->next) && canAccess(end)
     *     && start @ start->next
     *     && pred_sll(start->next, end, size - 1)
     *
     * We need to update the non ACSL variables used,
     * i.e. we take the name of the variable at position x and assign the result of
     * the operation in the function call argument at position x,
     * e.g. for function-definition: pred_sll(start, end, size), we apply the following two:
     * start = start->next; end = end; for function-call pred_sll(start->next, end, size - 1)
     */

    AcslPredicateDeclaration funDecl = pAcslPredicateApplicationPredicate.getPredicateDeclaration();

    AcslPredicate calledBody =
        getBodyForAcslPredicateApplicationPredicate(pAcslPredicateApplicationPredicate);
    checkNotNull(calledBody);

    // Add new stack-frame with the variables needed
    // We need to unpack the ACSLParameterDeclarations into usable C declarations first
    List<CParameterDeclaration> cParameterDeclarations =
        getCFunctionParametersFromDeclarationIn(pAcslPredicateApplicationPredicate);
    checkArgument(
        !cParameterDeclarations.isEmpty(),
        "Pure ACSL expressions (without C expression parameters) are currently not supported");

    // Resolve C arguments (not all ACSL arguments are needed C arguments)
    // We ignore pure ACSL arguments for now, as we expect them to be things like the size,
    // that are not limited)
    List<CExpression> cArguments = getCArgumentsFrom(pAcslPredicateApplicationPredicate);
    // It makes little sense to call a function without arguments that we can work with currently
    checkArgument(
        !cArguments.isEmpty(),
        "Pure ACSL expressions (without C expression arguments) are currently not supported");

    final AFunctionDeclaration funDeclOfInitialStackFrame =
        initialState.getStackFrameTopFunctionDefinition();

    // TODO: extract creation of pseudo C function-call into dedicated method
    FileLocation loc = pAcslPredicateApplicationPredicate.getFileLocation();
    String pseudoCFunName = funDecl.getName();
    CFANode predecessorNode = CFANode.newDummyCFANode();
    List<CType> pseudoCFunParameterTypes =
        cParameterDeclarations.stream()
            .map(d -> d.getType().getCanonicalType())
            .collect(ImmutableList.toImmutableList());
    // We model this without a return type as we model it through the visitors returns
    CFunctionDeclaration pseudoCFunDecl =
        new CFunctionDeclaration(
            loc,
            new CFunctionType(CVoidType.VOID, pseudoCFunParameterTypes, false),
            pseudoCFunName,
            cParameterDeclarations,
            ImmutableSet.of());
    String rawStatement = calledBody.toASTString();
    CFunctionEntryNode pseudoCFunEntryNode =
        new CFunctionEntryNode(loc, pseudoCFunDecl, null, Optional.empty());
    // TODO: i have no idea whether the CIdExpr is correct. But we don't access it anyway...
    CIdExpression pseudoCFunNameIdExpr =
        new CIdExpression(loc, CVoidType.VOID, pseudoCFunName, pseudoCFunDecl);
    CFunctionCallExpression pseudoFunCallExpr =
        new CFunctionCallExpression(
            loc, CVoidType.VOID, pseudoCFunNameIdExpr, cArguments, pseudoCFunDecl);
    CFunctionCall pseudoCFunctionCall = new CFunctionCallStatement(loc, pseudoFunCallExpr);
    CFunctionSummaryEdge pseudoCFunSummaryEdge =
        new CFunctionSummaryEdge(
            rawStatement,
            loc,
            predecessorNode,
            CFANode.newDummyCFANode(),
            pseudoCFunctionCall,
            pseudoCFunEntryNode);
    CFunctionCallEdge pseudoCFunCallEdge =
        new CFunctionCallEdge(
            rawStatement,
            loc,
            predecessorNode,
            pseudoCFunEntryNode,
            pseudoCFunctionCall,
            pseudoCFunSummaryEdge);
    SMGState stateWithNewStackFrameAndVars =
        callingTransfer.handleCFunctionCall(
            initialState, pseudoCFunCallEdge, pseudoCFunDecl, cArguments, cParameterDeclarations);

    Set<SMGState> processedStates =
        calledBody.accept(copyWithNewState(stateWithNewStackFrameAndVars));

    ImmutableSet.Builder<SMGState> finalStates = ImmutableSet.builder();
    for (SMGState processedState : processedStates) {
      assert processedState.getStackFrameTopFunctionName().equals(pseudoCFunName);
      SMGState processedStateAfterDrop = processedState.dropStackFrame();
      verify(
          processedStateAfterDrop.getStackFrameTopFunctionDefinition()
              == funDeclOfInitialStackFrame);
      finalStates.add(processedStateAfterDrop);
    }
    return finalStates.build();

    // TODO: we most likely don't need the code below, but lets keep it for now until i know more

    // The decl is really of type AcslPredicateDeclaration! This is fine for now
    /*
    if (funDeclsToBodies.containsKey(funDecl)) {
      AcslTerm body = funDeclsToBodies.get(funDecl);
      ImmutableList<? extends AParameterDeclaration> declParams = funDecl.getParameters();
      ImmutableList<AcslTerm> arguments = pAcslFunctionCallPredicate.getParameterExpressions();
      // Assign arguments to their declParams (to move the list forward; e.g. start = start->next)
      SMGState currentState = initialState;
      UnmodifiableIterator<? extends AParameterDeclaration> declParamsIter = declParams.iterator();
      UnmodifiableIterator<AcslTerm> argumentsIter = arguments.iterator();
      while (declParamsIter.hasNext()) {
        verify(argumentsIter.hasNext());

        AParameterDeclaration declaration = declParamsIter.next();
        AcslTerm argument = argumentsIter.next();

        // TODO: Wasn't there a builder for CExpressions?
        CAssignment assignment;

        if (argument instanceof AcslCLeftHandSideTerm lhsArg) {
          // e.g. start->next in funCall(start->next, ...)

          // This LHS is actually the right-hand-side of our new assignment!
          CLeftHandSide rhsCExpr = lhsArg.getCLeftHandSideExpression();
          assignment =
              new CExpressionAssignmentStatement(rhsCExpr.getFileLocation(), assignTo, rhsCExpr);

        } else if (argument instanceof AcslIdTerm idArg) {
          // e.g. variable in funCall(..., variable, ...)
          if (!idArg.getDeclaration().equals(oldDecl)) {
            assignment =
                new CExpressionAssignmentStatement(idArg.getFileLocation(), assignTo, idArg);

          } else {
            // We assume that assignments only happen in function calls and not in the function
            // body, hence if the assignments are equal (variable = variable;),
            // we don't have to do anything
            continue;
          }

        } else if (argument instanceof AcslTerm acslTermArg
            && acslTermArg.getExpressionType() instanceof AcslBuiltinLogicType) {
          // e.g. size - 1 in funCall(..., size - 1),
          // but size is a ACSL expression, not a C expression -> ignore (we don't track ACSL)
          continue;

        } else {
          throw new UnsupportedOperationException(
              "Expression "
                  + argument
                  + " of type "
                  + argument.getClass()
                  + " currently not supported");
        }

        // Assigning the new values might trigger a list materialization:
        // - No list materialization: 1 state is returned, just keep on processing.
        // - list materialization: 2 states returned; the first is the "not extended list", while
        // the second is the extended (with possibly unlimited extensions following) list.
        currentState = callingTransfer.handleStatement(originCfaEdge, assignment);
      }

      // TODO: we get the ACSL function body from the CFAMetadata based on the definition
      // Execute recursive function call with
      return body.accept(copyWithNewState(currentState));

    } else {
      // TODO: Reduce log level once this works
      logger.log(
          Level.WARNING, "Could not find ACSL function call body  " + pAcslFunctionCallPredicate);
      return ImmutableSet.of();
    }
     */
  }

  /**
   * Pulls out all C parameters of the declaration in the given {@link
   * AcslPredicateApplicationPredicate} in the order used in the declaration. Ignores pure ACSL
   * terms.
   */
  private static List<CParameterDeclaration> getCFunctionParametersFromDeclarationIn(
      AcslPredicateApplicationPredicate pAcslPredicateApplicationPredicate) {
    ImmutableList.Builder<CParameterDeclaration> declarationsCParametersBuilder =
        ImmutableList.builder();
    ImmutableList<AcslParameterDeclaration> acslParameters =
        pAcslPredicateApplicationPredicate.getPredicateDeclaration().getParameters();

    for (AcslParameterDeclaration acslParamDecl : checkNotNull(acslParameters)) {
      // We simply ignore pure ACSL parameter declarations
      acslParamDecl.asCParameterDeclaration().ifPresent(declarationsCParametersBuilder::add);
    }
    return declarationsCParametersBuilder.build();
  }

  /**
   * Returns all {@link CExpression} arguments from the arguments of the given {@link
   * AcslPredicateApplicationPredicate}. Preserves the order of arguments present in the given term.
   * Ignores pure ACSL terms.
   */
  private static List<CExpression> getCArgumentsFrom(
      AcslPredicateApplicationPredicate pAcslPredicateApplicationPredicate) {
    ImmutableList.Builder<CExpression> cArgumentsBuilder = ImmutableList.builder();
    for (AcslTerm param : pAcslPredicateApplicationPredicate.getParameters()) {
      asCExpression(param).ifPresent(cArgumentsBuilder::add);
    }
    return cArgumentsBuilder.build();
  }

  /**
   * Returns the given argument of the given {@link AcslCanAccessPredicate} as {@link CExpression}.
   * Arguments that are pure ACSL terms are not supported yet.
   */
  private static CExpression getCExpressionArgument(
      AExpression pArgument, AcslCanAccessPredicate pAcslCanAccessPredicate) {
    Optional<CExpression> cArgument =
        switch (pArgument) {
          case CExpression cExpression -> Optional.of(cExpression);
          case AcslTerm acslTerm -> asCExpression(acslTerm);
          default -> Optional.empty();
        };
    return cArgument.orElseThrow(
        () ->
            new UnsupportedOperationException(
                "Validation of ACSL arguments of "
                    + pAcslCanAccessPredicate
                    + " is not supported"));
  }

  /**
   * Returns the given ACSL term as {@link CExpression} if it is a C expression, i.e., a C
   * expression lifted to ACSL, a C variable, or a parameter with a C type. Pure ACSL terms are not
   * converted and result in {@link Optional#empty()}.
   */
  private static Optional<CExpression> asCExpression(AcslTerm pTerm) {
    return switch (pTerm) {
      case AcslCExpression cExpression -> Optional.of(cExpression.getCExpression());
      case AcslIdTerm idTerm ->
          switch (idTerm.getDeclaration()) {
            case AcslCVariableDeclaration cVariable ->
                Optional.of(
                    new CIdExpression(
                        idTerm.getFileLocation(), cVariable.getCVariableDeclaration()));
            case AcslParameterDeclaration parameter ->
                parameter
                    .asCParameterDeclaration()
                    .map(cParameter -> new CIdExpression(idTerm.getFileLocation(), cParameter));
            default -> Optional.empty();
          };
      case AcslIntegerLiteralTerm literal ->
          Optional.of(
              new CIntegerLiteralExpression(
                  literal.getFileLocation(), CNumericTypes.INT, literal.getValue()));
      default -> Optional.empty();
    };
  }

  /** Pulls the body for the given predicate application out of the known logic-definitions. */
  private AcslPredicate getBodyForAcslPredicateApplicationPredicate(
      AcslPredicateApplicationPredicate pAcslPredicateApplicationPredicate) {
    final AcslPredicateDeclaration calledPredicateDecl =
        pAcslPredicateApplicationPredicate.getPredicateDeclaration();
    AcslPredicate bodyForCalledFunction = null;
    for (AcslLogicDefinition logicDef : logicDefinitions) {
      if (logicDef instanceof AcslLogicPredicateDefinition pAcslLogicPredicateDefinition) {
        if (pAcslLogicPredicateDefinition.getDeclaration().equals(calledPredicateDecl)) {
          if (bodyForCalledFunction != null) {
            throw new IllegalArgumentException("Duplicate or conflicting ");
          }
          // TODO: return here and add a check that the logics and their defs never conflict in the
          // constructor of the visitor. Currently not important since we expect very few logic
          // defs.
          bodyForCalledFunction = pAcslLogicPredicateDefinition.getBody();
        }
      } else {
        throw new IllegalArgumentException(
            "ACSL logic definition of type "
                + logicDef.getClass()
                + " can not be handled currently. Failed definition: "
                + logicDef);
      }
    }

    if (bodyForCalledFunction == null) {
      throw new IllegalArgumentException("");
    }
    return bodyForCalledFunction;
  }

  @Override
  public Set<SMGState> visit(AcslFunctionCallPredicate pAcslFunctionCallPredicate)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * {@code \canAccess(x, len)}: Returns 1 if all memory regions from x to x+len-1 are allocated,
   * i.e., for all 0 <= i < len dereferencing x + i does not result in undefined behavior, and 0
   * otherwise. In this case, x must be a pointer and len must be of size_t. When evaluating x,
   * every evaluation of the indirection operator and pointer arithmetic is checked for conditions
   * imposed on them by the C standard (C11 §6.5.6, §6.5.3.2, §6.2.4, §7.22.3). If any of those
   * conditions is violated, {@code \canAccess(x, len)} immediately evaluates to false. For {@code
   * \canAccess(x, len)} checking all the conditions imposed by the C standard in practice means
   * that if the evaluation of x has undefined behavior related to MemSafety, e.g. for a[100] as
   * argument with a of type int*[50][50], the witness is still valid, however {@code \canAccess(x,
   * len)} returns false. Defined in <a
   * href="https://gitlab.com/sosy-lab/benchmarking/sv-witnesses/-/blob/7997352a6937ab042e08265c09f081210ab76a0f/user-guide/Witness-Format.md">witness
   * format version 2.4</a>, added via <a
   * href="https://gitlab.com/sosy-lab/benchmarking/sv-witnesses/-/merge_requests/111">MR!111 for
   * witness format 2.4</a>. TODO: use proper links once 2.4 is merged.
   */
  @Override
  public Set<SMGState> visit(AcslCanAccessPredicate pAcslCanAccessPredicate)
      throws CPATransferException {

    verify(!initialState.hasMemoryErrors());

    // We model \canAccess() by simply handing checking the returned memory errors
    // after checking the access
    CExpression exprTargetingMemory =
        getCExpressionArgument(
            pAcslCanAccessPredicate.getExpressionTargetingMemory(), pAcslCanAccessPredicate);
    CType accessTypeTargetingMemory = exprTargetingMemory.getExpressionType().getCanonicalType();

    // Access from exprTargetingMemory up to exprTargetingMemory + lenArgExpr - 1
    // needs to be guaranteed
    CExpression lenArgExpr =
        getCExpressionArgument(
            pAcslCanAccessPredicate.getLengthExpression(), pAcslCanAccessPredicate);

    ImmutableSet.Builder<SMGState> resultStates = ImmutableSet.builder();

    for (ValueAndSMGState lenArgAndState :
        new SMGCPAValueVisitor(evaluator, initialState, originCfaEdge, logger)
            .evaluate(lenArgExpr, lenArgExpr.getExpressionType().getCanonicalType())) {

      Value lenValueInBytes = lenArgAndState.getValue();

      for (SMGStateAndOptionalSMGObjectAndOffset targetMemory :
          exprTargetingMemory.accept(
              new SMGCPAAddressVisitor(
                  evaluator, lenArgAndState.getState(), originCfaEdge, logger))) {

        if (!targetMemory.hasSMGObjectAndOffset()) {
          // Invalid witness
          throw new IllegalArgumentException(
              "MemSafety witness failed to find the target memory for: " + pAcslCanAccessPredicate);
          // TODO: switch to the return. I only put the exception there to see whether this happens.
          // return ImmutableSet.of();
        }

        BooleanAndSMGState accessResult =
            checkCanAccessMemory(
                targetMemory.getOffsetForObject(),
                evaluator.multiplyBitOffsetValues(
                    lenValueInBytes,
                    BigInteger.valueOf(initialState.getMachineModel().getSizeofCharInBits())),
                targetMemory.getSMGObject(),
                targetMemory.getSMGState());

        if (accessResult.getBoolean()) {
          // Access is valid, but we also check for side effects
          SMGState accessValidState = checkNotNull(accessResult.getSMGState());
          if (!accessValidState.hasMemoryErrors()) {
            resultStates.add(accessValidState);
          }
        }
      }
    }

    return resultStates.build();
  }

  /**
   * Our separating conjunction {@code \separated(e1, ..., en)}, e.g. {@code \separated(start,
   * start->next)}, showing that the memory regions of the arguments are disjunct, for n >= 1. More
   * complex example: {@code \separated(\canAccess(start, sizeof(struct sll)), sll_seg(start->next,
   * end)))} Trying to access a memory region outside of ones arguments partition of memory is
   * interpreted as invalid dereference!
   */
  @Override
  public Set<SMGState> visit(
      AcslSeparateMemoryConjunctionPredicate pAcslSeparateMemoryConjunctionPredicate)
      throws CPATransferException {

    ImmutableSet.Builder<SMGState> resultStates = ImmutableSet.builder();
    List<AExpression> separatedExpressions =
        pAcslSeparateMemoryConjunctionPredicate.getSeparatedArguments();
    Collection<SMGState> currentStates = ImmutableList.of(initialState);

    for (AExpression separateExpr : separatedExpressions) {
      ImmutableSet.Builder<SMGState> evaluatedStates = ImmutableSet.builder();

      for (SMGState currentState : currentStates) {
        if (separateExpr instanceof CExpression sepCExpr) {
          // CExpression, e.g. just a pointer, but we might visit multiple heap sections
          // (SMGObjects)!
          // Resolve the memory, and make sure we don't touch it in the other arguments.
          // TODO: Tomas wraps at least the first argument always in \canAccess in SEAL.
          //  We might want to look into that as well.
          evaluatedStates.addAll(
              sepCExpr
                  .accept(new SMGCPAValueVisitor(evaluator, currentState, originCfaEdge, logger))
                  .stream()
                  .map(ValueAndSMGState::getState)
                  .collect(ImmutableList.toImmutableList()));

        } else if (separateExpr instanceof AcslPredicate pSepACSLPredicate) {
          // Handle via this visitor, as it is a ACSL expression.
          // E.g. a \canAccess() or a recursive predicate
          // TODO: resolve all touched memory in those via a class variable somehow
          //  (this is actually hard...), e.g. via a new CExpression visitor that gives all partial
          //  expressions or all visited objects, e.g. for start->next->*(nested.data) we get the
          //  expressions and or SMGObjects for start, start->next, start->next->nested,
          //  and start->next->*(nested.data). We could modify the AddressVisitor for this purpose.
          //  This must collect all abstracted memory without unfolding though, as otherwise we
          //  might loop endlessly. Alternatively, collect all memory when working on it,
          //  but this is a giant hassle with many chances of fucking it up.
          //  This also needs to be used in the CExpression case above.

          evaluatedStates.addAll(pSepACSLPredicate.accept(copyWithNewState(currentState)));

        } else {
          throw new UnsupportedOperationException(
              "Unhandled type of argument in separated conjunction: " + separateExpr);
        }
      }
      currentStates = evaluatedStates.build();
    }

    return resultStates.build();
  }

  @Override
  public Set<SMGState> visit(AcslTypeVariableDeclaration pDecl) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslParameterDeclaration pAcslParameterDeclaration)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslUnaryTerm pAcslUnaryTerm) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslStringLiteralTerm pAcslStringLiteralTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslRealLiteralTerm pAcslRealLiteralTerm) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslCharLiteralTerm pAcslCharLiteralTerm) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslIntegerLiteralTerm pAcslIntegerLiteralTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslBooleanLiteralTerm pAcslBooleanLiteralTerm) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslBinaryTerm pAcslBinaryTerm) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslIdTerm pAcslBinaryTerm) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslOldTerm pAcslOldTerm) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslResultTerm pAcslResultTerm) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslAtTerm pAcslAtTerm) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslTernaryTerm pAcslTernaryTerm) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslFunctionCallTerm pAcslFunctionCallTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslArraySubscriptTerm pAcslArraySubscriptTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslCExpression pAcslCExpression) throws CPATransferException {
    // C expressions wrapped in Acsl terms, e.g.: start->next != 0
    return pAcslCExpression.getCExpression().accept(this);
  }

  @Override
  public Set<SMGState> visit(CArrayDesignator pArrayDesignator) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CArrayRangeDesignator pArrayRangeDesignator)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CFieldDesignator pFieldDesignator) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CTypeIdExpression pIastTypeIdExpression) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CInitializerList pInitializerList) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CDesignatedInitializer pCStructInitializerPart)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CFieldReference pIastFieldReference) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CPointerExpression pointerExpression) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CComplexCastExpression complexCastExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CComplexTypeDeclaration pDecl) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CTypeDefDeclaration pDecl) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(CEnumerator pDecl) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(JClassInstanceCreation pJClassInstanceCreation)
      throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JBooleanLiteralExpression pJBooleanLiteralExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JArrayCreationExpression pJArrayCreationExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JArrayInitializer pJArrayInitializer) throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JArrayLengthExpression pJArrayLengthExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JVariableRunTimeType pJThisRunTimeType) throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType)
      throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JNullLiteralExpression pJNullLiteralExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JEnumConstantExpression pJEnumConstantExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JThisExpression pThisExpression) throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(JClassLiteralExpression pJClassLiteralExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("Java is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(SvLibVariableDeclaration pSvLibVariableDeclaration)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(SvLibParameterDeclaration pSvLibParameterDeclaration)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibFunctionCallExpression pSvLibFunctionCallExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibFunctionDeclaration pSvLibFunctionDeclaration)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibParameterDeclaration pSvLibParameterDeclaration)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibVariableDeclarationTuple pSvLibVariableDeclarationTuple)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibTermAssignmentCfaStatement pSvLibTermAssignmentCfaStatement)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(
      SvLibFunctionCallAssignmentStatement pSvLibFunctionCallAssignmentStatement)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibIdTermTuple pSvLibIdTermTuple) throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibAtTerm pSvLibAtTerm) throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibSymbolApplicationTerm pSvLibSymbolApplicationTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibIdTerm pSvLibIdTerm) throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibIntegerConstantTerm pSvLibIntegerConstantTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(
      SvLibSymbolApplicationRelationalTerm pSvLibSymbolApplicationRelationalTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibBooleanConstantTerm pSvLibBooleanConstantTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibRealConstantTerm pSvLibRealConstantTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibArrayConstantTerm pSvLibArrayConstantTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibBitVectorConstantTerm pSvLibBitVectorConstantTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibFloatingPointConstantTerm pSvLibFloatingPointConstantTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibRoundingModeConstantTerm pSvLibRoundingModeConstantTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibTagReference pSvLibTagReference) throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibCheckTrueTag pSvLibCheckTrueTag) throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibRequiresTag pSvLibRequiresTag) throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibEnsuresTag pSvLibEnsuresTag) throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibInvariantTag pSvLibInvariantTag) throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibNotRecurringTag pSvLibNotRecurringTag)
      throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> accept(SvLibRecurringTag pSvLibRecurringTag) throws CPATransferException {
    throw new UnsupportedOperationException("SV-LIB is not supported by the SMG-CPA");
  }

  @Override
  public Set<SMGState> visit(AcslBuiltinLabel pAcslBuiltinLabel) throws CPATransferException {
    throw new UnsupportedOperationException("Labels should not be visited!");
  }

  @Override
  public Set<SMGState> visit(AcslProgramLabel pAcslProgramLabel) throws CPATransferException {
    throw new UnsupportedOperationException("Labels should not be visited!");
  }

  /**
   * Evaluates C assumptions in the SMGCPA and returns no elements of not fulfilled, else the
   * fulfilling states. This might materialize list elements (e.g. more than one returned element).
   */
  private Set<SMGState> handleAssumptionWithSimplification(SMGState state, CExpression pExpression)
      throws CPATransferException {

    Pair<AExpression, Boolean> simplifiedExpression =
        simplifyAssumption(pExpression, truthAssumption);
    final CExpression expression = (CExpression) simplifiedExpression.getFirst();
    final boolean updatedTruthValue = simplifiedExpression.getSecond();

    ImmutableSet.Builder<SMGState> resultStateBuilder = ImmutableSet.builder();
    SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, state, originCfaEdge, logger);
    // Get the value of the expression (either true[1], false[0], or unknown)
    // Note: this might materialize an abstracted linked-list (more than 1 element in the
    //  resultStateBuilder)
    for (ValueAndSMGState valueAndState :
        vv.evaluate(expression, SMGCPAExpressionEvaluator.getCanonicalType(expression))) {
      Value value = valueAndState.getValue();
      SMGState currentState = valueAndState.getState();

      if (representsBoolean(value, updatedTruthValue)) {
        // We do not know more than before, and the assumption is fulfilled.
        // Return the state from the value visitor.
        // This state might be materialized. It might happen that e.g. 2 materialized states are
        // returned and one is rejected, while the other fulfills.
        resultStateBuilder.add(currentState);
      }
    }

    ImmutableSet<SMGState> resultStates = resultStateBuilder.build();
    if (resultStates.isEmpty()) {
      // Assumption not fulfilled
      logger.log(
          Level.WARNING, () -> "Assumption " + expression + " not fulfilled in ACSL visitor");
    }
    return resultStates;
  }
}
