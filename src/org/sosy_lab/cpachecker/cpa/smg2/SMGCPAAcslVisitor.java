// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation.simplifyAssumption;
import static org.sosy_lab.cpachecker.cpa.smg2.SMGTransferRelation.representsBoolean;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate.AcslBinaryPredicateOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpressionTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCLeftHandSideTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionCallTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicPredicateDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSetEmpty;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSetTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslProgramLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslResultTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
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
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTermTuple;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermAssignmentCfaStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclarationTuple;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibInvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibSymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

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
 * Predicate to parse:
 * pred_sll(sll * start, sll * end, int size):
 *   size == 1 ? start != 0 && start->next == 0 && start == end
 *   : start != 0 && start->next != start && start->next != 0
 *  && pred_sll(start->next, end, size - 1)
 *
 *
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

  // Saves the last acsl function call to ensure we traverse a single function recursively
  private final Optional<AcslFunctionDeclaration> currentFunction = Optional.empty();

  private final SMGState initialState;

  private final SMGCPAExpressionEvaluator evaluator;
  private final LogManagerWithoutDuplicates logger;
  private final SMGOptions options;

  private SMGCPAAcslVisitor(
      SMGState pInitialState,
      boolean pTruthAssumption,
      SMGOptions pOptions,
      LogManagerWithoutDuplicates pLogger,
      SMGCPAExpressionEvaluator pEvaluator) {
    initialState = checkNotNull(pInitialState);
    logger = checkNotNull(pLogger);
    options = checkNotNull(pOptions);
    truthAssumption = pTruthAssumption;
    evaluator = pEvaluator;
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
      try {
        return handleAssumptionWithSimplification(
            initialState,
            new DummyCFAEdge(CFANode.newDummyCFANode(), CFANode.newDummyCFANode()),
            cExpr);
      } catch (InterruptedException pE) {
        throw new RuntimeException(pE);
      }
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
    res.addAll(exp.getOperand1().accept_(this));
    res.addAll(exp.getOperand2().accept_(this));

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
        flattenedListBuilder.add((AcslPredicate) exp.getOperand1());
      }
      if (exp.getOperand2() instanceof AcslBinaryPredicate op2
          && op2.getOperator().equals(opToFlatten)) {
        List<AcslPredicate> nestedFlattened2 =
            flattenNestedBinaryPredicatesByGivenOperator(op2, opToFlatten);
        checkState(nestedFlattened2.size() >= 2);
        flattenedListBuilder.addAll(nestedFlattened2);
      } else {
        flattenedListBuilder.add((AcslPredicate) exp.getOperand2());
      }
      return flattenedListBuilder.build();
    }

    return ImmutableList.of();
  }

  @Override
  public Set<SMGState> visit(AcslInitializerExpression pInitializerExpression)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslLogicFunctionDefinition pAcslLogicFunctionDefinition)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslLogicPredicateDefinition pAcslLogicPredicateDefinition)
      throws CPATransferException {
    /*
     * For example with a nested function definition;
     *  e.g. a recursive function 'pred_sll' used to describe a linked-list in a witness:
     * pred_sll(sll * start, sll * end, int size):
     *   size == 1 ? start->next == 0 && start == end
     *   : start != 0 && start->next != start && start->next != 0
     *     && pred_sll(start->next, end, size - 1)
     */

    if (pAcslLogicPredicateDefinition.getDeclaration() != null
        && !pAcslLogicPredicateDefinition.getDeclaration().getParameters().isEmpty()) {
      logger.log(
          Level.WARNING,
          "ACSL logic predicate function definition argument validation not yet implemented!");
      // TODO: validate arguments as well if present
    }
    return pAcslLogicPredicateDefinition.getBody().accept(this);
  }

  @Override
  public Set<SMGState> visit(AcslMemoryLocationSetEmpty pAcslMemoryLocationSetEmpty)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslMemoryLocationSetTerm pAcslMemoryLocationSetTerm)
      throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslIdPredicate pAcslIdPredicate) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslBinaryTermPredicate pAcslBinaryTermPredicate)
      throws CPATransferException {
    // E.g. ITE conditions like 'size == 1'

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
  public Set<SMGState> visit(AcslForallPredicate pForallPredicate) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslExistsPredicate pAcslExistsPredicate) throws CPATransferException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslFunctionCallPredicate pAcslFunctionCallPredicate) {
    // Most likely a recursive function call, e.g.: pred_sll(start->next, end, size - 1)

    AcslFunctionDeclaration funDecl = pAcslFunctionCallPredicate.getDeclaration();

    AcslTerm funNameExpr = pAcslFunctionCallPredicate.getFunctionNameExpression();
    if (funNameExpr != null && funNameExpr instanceof AcslIdTerm funNameId) {
      if (!funNameId.getDeclaration().equals(funDecl)
          || funDecl.getName() == null
          || currentFunction.isEmpty()
          || !funDecl.equals(currentFunction.orElseThrow())) {
        logger.log(
            Level.WARNING,
            "Could not establish recursive ACSL predicate with " + pAcslFunctionCallPredicate);
        return ImmutableSet.of();
      }
    } else {
      logger.log(
          Level.WARNING,
          "Could not establish recursive ACSL predicate with " + pAcslFunctionCallPredicate);
      return ImmutableSet.of();
    }

    ImmutableList<? extends AParameterDeclaration> declParams = funDecl.getParameters();
    ImmutableList<AcslTerm> params = pAcslFunctionCallPredicate.getParameterExpressions();
    // Assign params to their declParams (to move the list forward)
    // TODO:

    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslPredicateTerm pAcslPredicateTerm) throws CPATransferException {
    // Terms wrapped as predicates (i.e. they return only boolean results)
    // For example AcslCExpressionTerm with a C expression like:  start->next != 0
    return pAcslPredicateTerm.getTerm().accept(this);
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
  public Set<SMGState> visit(AcslCLeftHandSideTerm pAcslCLeftHandSideTerm) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Set<SMGState> visit(AcslCExpressionTerm pAcslCExpressionTerm) throws CPATransferException {
    // C expressions wrapped in Acsl terms, e.g.: start->next != 0
    return pAcslCExpressionTerm.getCExpression().accept(this);
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
  public Set<SMGState> visit(AcslBinaryPredicateOperator pDecl) throws CPATransferException {
    throw new UnsupportedOperationException("Operators should not be visited!");
  }

  @Override
  public Set<SMGState> visit(AcslBinaryTermOperator pDecl) throws CPATransferException {
    throw new UnsupportedOperationException("Operators should not be visited!");
  }

  @Override
  public Set<SMGState> visit(AcslBinaryTermExpressionOperator pDecl) throws CPATransferException {
    throw new UnsupportedOperationException("Operators should not be visited!");
  }

  @Override
  public Set<SMGState> visit(AcslUnaryTermOperator pDecl) throws CPATransferException {
    throw new UnsupportedOperationException("Operators should not be visited!");
  }

  @Override
  public Set<SMGState> visit(AcslUnaryExpressionOperator pDecl) throws CPATransferException {
    throw new UnsupportedOperationException("Operators should not be visited!");
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
  private Set<SMGState> handleAssumptionWithSimplification(
      SMGState state, CFAEdge cfaEdge, CExpression pExpression)
      throws CPATransferException, InterruptedException {

    Pair<AExpression, Boolean> simplifiedExpression =
        simplifyAssumption(pExpression, truthAssumption);
    final CExpression expression = (CExpression) simplifiedExpression.getFirst();
    final boolean updatedTruthValue = simplifiedExpression.getSecond();

    ImmutableSet.Builder<SMGState> resultStateBuilder = ImmutableSet.builder();
    SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger, options);
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
