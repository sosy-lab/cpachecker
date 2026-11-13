// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cfa.ast.FileLocation.DUMMY;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BITWISE_AND;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.BITWISE_OR;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.GREATER_EQUAL;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.LESS_THAN;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.MULTIPLY;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.PLUS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ONE;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.ZERO;
import static org.sosy_lab.cpachecker.core.algorithm.termination.TerminationUtils.createDereferencedVariable;
import static org.sosy_lab.cpachecker.core.algorithm.termination.TerminationUtils.createPrimedVariable;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.AffineFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.LexicographicRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.LinearRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.NestedRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import java.io.Serial;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CFormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.basicimpl.FormulaCreator;

public class RankingRelationBuilder {

  private final LogManager logger;

  private final CBinaryExpressionBuilder cExpressionBuilder;

  private final FormulaManagerView fmgr;

  private final BooleanFormulaManagerView bfmgr;

  private final IntegerFormulaManagerView ifmgr;

  private final NumeralFormula zero;

  private final FormulaCreator<Term, ?, ?, ?> formulaCreator;

  /** Names the UFs of the heap encoding with UFs, from which the lassos are built. */
  private final TypeHandlerWithPointerAliasing typeHandler;

  public RankingRelationBuilder(
      MachineModel pMachineModel,
      LogManager pLogger,
      FormulaManagerView pFormulaManagerView,
      FormulaCreator<Term, ?, ?, ?> pFormulaCreator,
      Configuration pConfig)
      throws InvalidConfigurationException {
    logger = checkNotNull(pLogger);
    cExpressionBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
    fmgr = checkNotNull(pFormulaManagerView);
    bfmgr = fmgr.getBooleanFormulaManager();
    ifmgr = fmgr.getIntegerFormulaManager();
    zero = ifmgr.makeNumber(0L);
    formulaCreator = checkNotNull(pFormulaCreator);
    typeHandler =
        new TypeHandlerWithPointerAliasing(
            pLogger, pMachineModel, new CFormulaEncodingWithPointerAliasingOptions(pConfig));
  }

  public RankingRelation fromTerminationArgument(
      TerminationArgument pTerminationArgument, Set<CVariableDeclaration> pRelevantVariables)
      throws RankingRelationException {
    RankingRelation rankingRelation;
    try {
      rankingRelation =
          fromRankingFunction(pRelevantVariables, pTerminationArgument.getRankingFunction());
    } catch (UnrecognizedCodeException e) {
      throw new RankingRelationException(e);
    }

    Collection<BooleanFormula> supportingInvariants =
        extractSupportingInvariants(pTerminationArgument, pRelevantVariables);
    return rankingRelation.withSupportingInvariants(supportingInvariants);
  }

  /**
   * Returns the C expressions of the variables of the ranking function and the supporting
   * invariants of the given termination argument, e.g., {@code *p} for the heap cell that the
   * pointer {@code p} points to. Variables without a C expression, e.g., a heap cell at an offset
   * of a pointer, are left out.
   */
  public ImmutableMap<IProgramVar, CExpression> getCExpressionsOfVariables(
      TerminationArgument pTerminationArgument, Set<CVariableDeclaration> pRelevantVariables) {
    ImmutableMap.Builder<IProgramVar, CExpression> cExpressions = ImmutableMap.builder();
    for (IProgramVar programVar :
        FluentIterable.from(pTerminationArgument.getRankingFunction().getVariables())
            .append(
                FluentIterable.from(pTerminationArgument.getSupportingInvariants())
                    .transformAndConcat(SupportingInvariant::getVariables))) {
      RankVar rankVar = (RankVar) programVar; // Only RankVars were passed to LassoRanker!
      try {
        cExpressions.put(rankVar, getVariable(rankVar, pRelevantVariables).getSecondNotNull());
      } catch (RankingRelationException e) {
        logger.logDebugException(e, "No C expression for " + rankVar);
      }
    }
    // A variable can occur in the ranking function and in several supporting invariants
    return cExpressions.buildKeepingLast();
  }

  private Collection<BooleanFormula> extractSupportingInvariants(
      TerminationArgument pTerminationArgument, Set<CVariableDeclaration> pRelevantVariables) {
    Collection<BooleanFormula> supportingInvariants = new ArrayList<>();
    for (SupportingInvariant supportingInvariant : pTerminationArgument.getSupportingInvariants()) {

      RankingRelationComponents components;
      try {
        components = createRankingRelationComponents(supportingInvariant, pRelevantVariables);
      } catch (RankingRelationException e) {
        logger.logDebugException(e, "Could not process " + supportingInvariant);
        continue; // just skip this invariant
      }

      BooleanFormula invariantFormula;
      if (supportingInvariant.strict) {
        invariantFormula = fmgr.makeGreaterThan(components.getUnprimedFormula(), zero, true);
      } else {
        invariantFormula = fmgr.makeGreaterOrEqual(components.getUnprimedFormula(), zero, true);
      }

      supportingInvariants.add(fmgr.uninstantiate(invariantFormula));
    }

    return supportingInvariants;
  }

  private RankingRelation fromRankingFunction(
      Set<CVariableDeclaration> pRelevantVariables, RankingFunction rankingFunction)
      throws UnrecognizedCodeException, RankingRelationException {
    return switch (rankingFunction) {
      case LinearRankingFunction linearRankingFunction -> {
        AffineFunction function = linearRankingFunction.getComponent();
        yield fromAffineFunction(pRelevantVariables, function);
      }
      case LexicographicRankingFunction lexicographicRankingFunction ->
          fromLexicographicRankingFunction(lexicographicRankingFunction, pRelevantVariables);
      case NestedRankingFunction nestedRankingFunction ->
          fromNestedRankingFunction(nestedRankingFunction, pRelevantVariables);
      default -> throw new UnsupportedOperationException(rankingFunction.getName());
    };
  }

  private RankingRelation fromLexicographicRankingFunction(
      LexicographicRankingFunction rankingFunction, Set<CVariableDeclaration> pRelevantVariables)
      throws UnrecognizedCodeException, RankingRelationException {

    CExpression cExpression = CIntegerLiteralExpression.ZERO;
    List<BooleanFormula> formulas = new ArrayList<>();

    for (RankingFunction component : rankingFunction.getComponents()) {
      RankingRelation rankingRelation = fromRankingFunction(pRelevantVariables, component);
      CExpression cExpressionComponent = rankingRelation.asCExpression();
      cExpression =
          cExpressionBuilder.buildBinaryExpression(cExpression, cExpressionComponent, BITWISE_OR);
      formulas.add(rankingRelation.asFormula());
    }

    BooleanFormula formula = bfmgr.or(formulas);
    return new RankingRelation(cExpression, formula, cExpressionBuilder, fmgr);
  }

  private RankingRelation fromNestedRankingFunction(
      NestedRankingFunction pRankingFunction, Set<CVariableDeclaration> pRelevantVariables)
      throws UnrecognizedCodeException, RankingRelationException {
    Preconditions.checkArgument(pRankingFunction.getComponents().length > 0);

    BooleanFormula phaseConditionFormula = bfmgr.makeTrue();
    CExpression phaseConditionExpression = CIntegerLiteralExpression.ONE;

    List<CExpression> componentExpressions = new ArrayList<>();
    List<BooleanFormula> componentFormulas = new ArrayList<>();

    for (AffineFunction component : pRankingFunction.getComponents()) {
      RankingRelation componentRelation = fromAffineFunction(pRelevantVariables, component);

      CBinaryExpression componentExpression =
          cExpressionBuilder.buildBinaryExpression(
              phaseConditionExpression, componentRelation.asCExpression(), BITWISE_AND);
      componentExpressions.add(componentExpression);
      BooleanFormula componentFormula =
          fmgr.makeAnd(phaseConditionFormula, componentRelation.asFormula());
      componentFormulas.add(componentFormula);

      // update phase condition
      RankingRelationComponents rankingRelationComponents =
          createRankingRelationComponents(component, pRelevantVariables);

      BooleanFormula unprimedLessThanZeroFormula =
          fmgr.makeLessThan(rankingRelationComponents.getUnprimedFormula(), zero, true);
      phaseConditionFormula = fmgr.makeAnd(phaseConditionFormula, unprimedLessThanZeroFormula);

      CExpression unprimedLessThanZeroExpression =
          cExpressionBuilder.buildBinaryExpression(
              rankingRelationComponents.getUnprimedExpression().orElse(ZERO), ZERO, LESS_THAN);
      phaseConditionExpression =
          cExpressionBuilder.buildBinaryExpression(
              phaseConditionExpression, unprimedLessThanZeroExpression, BITWISE_AND);
    }

    BooleanFormula formula = fmgr.getBooleanFormulaManager().or(componentFormulas);
    CExpression expression =
        componentExpressions.stream()
            .reduce(
                (op1, op2) ->
                    cExpressionBuilder.buildBinaryExpressionUnchecked(op1, op2, BITWISE_OR))
            .orElseThrow();

    return new RankingRelation(expression, formula, cExpressionBuilder, fmgr);
  }

  private RankingRelation fromAffineFunction(
      Set<CVariableDeclaration> pRelevantVariables, AffineFunction function)
      throws UnrecognizedCodeException, RankingRelationException {
    RankingRelationComponents components =
        createRankingRelationComponents(function, pRelevantVariables);

    Optional<CExpression> rankingRelation = createRankingRelationExpression(components);
    BooleanFormula rankingRelationFormula = createRankingRelationFormula(components);

    return new RankingRelation(rankingRelation, rankingRelationFormula, cExpressionBuilder, fmgr);
  }

  private Optional<CExpression> createRankingRelationExpression(
      RankingRelationComponents components) throws UnrecognizedCodeException {
    Optional<CExpression> unprimedFunction = components.getUnprimedExpression();
    Optional<CExpression> primedFunction = components.getPrimedExpression();

    if (unprimedFunction.isPresent() && primedFunction.isPresent()) {
      CExpression unprimedGreatorThanZero =
          cExpressionBuilder.buildBinaryExpression(
              primedFunction.orElseThrow(), ZERO, GREATER_EQUAL);
      CExpression primedLessThanUnprimed =
          cExpressionBuilder.buildBinaryExpression(
              unprimedFunction.orElseThrow(), primedFunction.orElseThrow(), LESS_THAN);

      CBinaryExpression rankingRelation =
          cExpressionBuilder.buildBinaryExpression(
              unprimedGreatorThanZero, primedLessThanUnprimed, BITWISE_AND);
      return Optional.of(rankingRelation);

    } else {
      return Optional.empty();
    }
  }

  private RankingRelationComponents createRankingRelationComponents(
      AffineFunction function, Set<CVariableDeclaration> pRelevantVariables)
      throws RankingRelationException {
    // f(x')
    Optional<CExpression> primedFunction = createLiteral(function.getConstant());
    List<NumeralFormula> primedFormulaSummands = new ArrayList<>();
    primedFormulaSummands.add(ifmgr.makeNumber(function.getConstant()));

    // f(x)
    Optional<CExpression> unprimedFunction = createLiteral(function.getConstant());
    List<NumeralFormula> unprimedFormulaSummands = new ArrayList<>();
    unprimedFormulaSummands.add(ifmgr.makeNumber(function.getConstant()));

    for (IProgramVar programVar : function.getVariables()) {
      RankVar rankVar = (RankVar) programVar; // Only RankVars were passed to LassoRanker!
      BigInteger coefficient = function.get(rankVar);
      Optional<CExpression> cCoefficient = createLiteral(coefficient);
      Pair<CIdExpression, CExpression> variables = getVariable(rankVar, pRelevantVariables);

      CIdExpression primedVariable = variables.getFirstNotNull();
      CExpression variable = variables.getSecondNotNull();

      if (primedFunction.isPresent() && unprimedFunction.isPresent() && cCoefficient.isPresent()) {
        try {
          primedFunction =
              Optional.of(
                  addSummand(
                      primedFunction.orElseThrow(), cCoefficient.orElseThrow(), primedVariable));
          unprimedFunction =
              Optional.of(
                  addSummand(unprimedFunction.orElseThrow(), cCoefficient.orElseThrow(), variable));

        } catch (UnrecognizedCodeException e) {
          // some ranking function cannot be represented by C expressions
          // e.g. multiplication of pointers
          primedFunction = Optional.empty();
          unprimedFunction = Optional.empty();
        }

      } else {
        primedFunction = Optional.empty();
        unprimedFunction = Optional.empty();
      }

      NumeralFormula unprimedVariableFormula = encapsulate(rankVar.getTerm());
      String primedVariableName = primedVariable.getDeclaration().getQualifiedName();
      FormulaType<NumeralFormula> formulaType = fmgr.getFormulaType(unprimedVariableFormula);
      NumeralFormula primedVariableFormula = fmgr.makeVariable(formulaType, primedVariableName);
      primedFormulaSummands.add(createSummand(coefficient, primedVariableFormula));
      unprimedFormulaSummands.add(createSummand(coefficient, unprimedVariableFormula));
    }

    return new RankingRelationComponents(
        unprimedFunction, primedFunction, unprimedFormulaSummands, primedFormulaSummands);
  }

  private CExpression addSummand(CExpression pSum, CExpression pCoefficient, CExpression pVariable)
      throws UnrecognizedCodeException {

    CExpression summand;
    if (pCoefficient.equals(ONE)) {
      summand = pVariable;
    } else {
      summand = cExpressionBuilder.buildBinaryExpression(pCoefficient, pVariable, MULTIPLY);
    }
    return cExpressionBuilder.buildBinaryExpression(pSum, summand, PLUS);
  }

  private NumeralFormula encapsulate(Term pTerm) {
    Sort sort = pTerm.getSort();
    assert sort.isNumericSort();

    FormulaType<? extends NumeralFormula> type;
    if (sort.getName().equalsIgnoreCase("int")) {
      type = FormulaType.IntegerType;
    } else if (sort.getName().equalsIgnoreCase("real")) {
      type = FormulaType.RationalType;
    } else {
      throw new AssertionError(sort);
    }

    return formulaCreator.encapsulate(type, pTerm);
  }

  /**
   * Returns the primed and unprimed variable represented as {@link CExpression}s for a variable of
   * the ranking function.
   *
   * @param pRankVar the variable of the ranking function to get the primed and unprimed variable
   *     for
   * @param pRelevantVariables all variable declarations of the original program
   * @return a Pair consisting of the primed and unprimed variable
   * @throws RankingRelationException if it is not possible to create a {@link CExpression} from
   *     <code>pRankVar</code>code
   */
  private Pair<CIdExpression, CExpression> getVariable(
      RankVar pRankVar, Set<CVariableDeclaration> pRelevantVariables)
      throws RankingRelationException {
    String variableName = pRankVar.getGloballyUniqueId();
    Optional<CVariableDeclaration> variableDecl =
        pRelevantVariables.stream()
            .filter(v -> v.getQualifiedName().equals(variableName))
            .findAny();

    if (variableDecl.isPresent()) {
      CVariableDeclaration primedVariableDecl = createPrimedVariable(variableDecl.orElseThrow());
      CIdExpression primedVariable = new CIdExpression(DUMMY, primedVariableDecl);
      CIdExpression variable = new CIdExpression(DUMMY, variableDecl.orElseThrow());
      return Pair.of(primedVariable, variable);

    } else if (pRankVar.getTerm() instanceof ApplicationTerm heapCell
        && !heapCell.getFunction().isInterpreted()
        && heapCell.getParameters().length == 1
        && heapCell.getParameters()[0] instanceof ApplicationTerm innerVariableTerm) {
      // a heap cell of the heap encoding with UFs, e.g., (*int |main::p|) for *p

      String innerVariableName = innerVariableTerm.getFunction().getName();
      RankVar innerDummyRankVar =
          new RankVar(innerVariableName, pRankVar.isGlobal(), innerVariableTerm);
      Pair<CIdExpression, CExpression> innerVariables =
          getVariable(innerDummyRankVar, pRelevantVariables);

      CSimpleDeclaration innerPrimedVariable = innerVariables.getFirstNotNull().getDeclaration();
      if (!isHeapCellOfPointer(heapCell, innerPrimedVariable)) {
        throw new RankingRelationException("Cannot handle term " + pRankVar.getTerm());
      }
      CExpression innerVariable = innerVariables.getSecondNotNull();
      CVariableDeclaration primedVariableDecl = createDereferencedVariable(innerPrimedVariable);
      CExpression variable =
          new CPointerExpression(DUMMY, primedVariableDecl.getType(), innerVariable);
      CIdExpression primedVariable = new CIdExpression(DUMMY, primedVariableDecl);
      return Pair.of(primedVariable, variable);

    } else {
      // e.g. array, binary operators, etc. are not supported
      throw new RankingRelationException("Cannot handle term " + pRankVar.getTerm());
    }
  }

  /**
   * Returns whether the given application of a UF of the heap encoding is the heap cell that the
   * given pointer points to, i.e., whether the UF is the one for the target type of the pointer.
   */
  private boolean isHeapCellOfPointer(ApplicationTerm pHeapCell, CSimpleDeclaration pPointer) {
    String heapName = pHeapCell.getFunction().getName();
    // A cell of the byte array of the heap is a single byte, not a value of the target type
    return pPointer.getType() instanceof CPointerType pointerType
        && !TypeHandlerWithPointerAliasing.isByteArrayAccessName(heapName)
        && typeHandler.getPointerAccessNameForType(pointerType.getType()).equals(heapName);
  }

  private static Optional<CExpression> createLiteral(BigInteger value) {
    if (value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0
        && value.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0) {
      return Optional.of(
          CIntegerLiteralExpression.createDummyLiteral(
              value.longValueExact(), CNumericTypes.LONG_LONG_INT));
    } else {
      return Optional.empty();
    }
  }

  private BooleanFormula createRankingRelationFormula(RankingRelationComponents components) {

    NumeralFormula primedFormula = components.getPrimedFormula();
    NumeralFormula unprimedFormula = components.getUnprimedFormula();

    BooleanFormula unprimedGreatorThanZeroFormula =
        fmgr.makeGreaterOrEqual(primedFormula, zero, true);
    BooleanFormula primedLessThanUnprimedFormula =
        fmgr.makeLessThan(unprimedFormula, primedFormula, true);

    BooleanFormula rankingRelationFormula =
        fmgr.makeAnd(unprimedGreatorThanZeroFormula, primedLessThanUnprimedFormula);
    return rankingRelationFormula;
  }

  private NumeralFormula createSummand(BigInteger pCoefficient, NumeralFormula pVariable) {

    if (pCoefficient.equals(BigInteger.ONE)) {
      return pVariable;
    } else {
      IntegerFormula coefficient = ifmgr.makeNumber(pCoefficient);
      return fmgr.makeMultiply(coefficient, pVariable);
    }
  }

  private final class RankingRelationComponents {

    private final Optional<CExpression> unprimedExpression;
    private final Optional<CExpression> primedExpression;
    private final List<NumeralFormula> unprimedFormulaSummands;
    private final List<NumeralFormula> primedFormulaSummands;

    RankingRelationComponents(
        Optional<CExpression> pUnprimedFunction,
        Optional<CExpression> pPrimedFunction,
        List<NumeralFormula> pUnprimedFormulaSummands,
        List<NumeralFormula> pPrimedFormulaSummands) {
      unprimedExpression = pUnprimedFunction;
      primedExpression = pPrimedFunction;
      unprimedFormulaSummands = pUnprimedFormulaSummands;
      primedFormulaSummands = pPrimedFormulaSummands;
    }

    Optional<CExpression> getPrimedExpression() {
      return primedExpression;
    }

    Optional<CExpression> getUnprimedExpression() {
      return unprimedExpression;
    }

    NumeralFormula getPrimedFormula() {
      return sum(primedFormulaSummands);
    }

    NumeralFormula getUnprimedFormula() {
      return sum(unprimedFormulaSummands);
    }

    private NumeralFormula sum(Collection<NumeralFormula> operands) {
      return operands.stream().reduce(zero, fmgr::makePlus);
    }
  }

  public static class RankingRelationException extends Exception {

    @Serial private static final long serialVersionUID = 1L;

    public RankingRelationException(String message) {
      super(message);
    }

    public RankingRelationException(Exception e) {
      super(e);
    }
  }
}
