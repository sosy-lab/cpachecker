// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.k3toformula;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.FormatMethod;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3ProcedureCallEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3ProcedureEntryNode;
import org.sosy_lab.cpachecker.cfa.model.k3.K3ProcedureReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3ProcedureSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.LanguageToSmtConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

@SuppressWarnings("unused")
public class K3ToFormulaConverter implements LanguageToSmtConverter {

  private final FormulaManagerView fmgr;
  private final FormulaEncodingOptions options;
  private final Optional<VariableClassification> variableClassification;
  private final BooleanFormulaManagerView bfmgr;
  private final BitvectorFormulaManagerView efmgr;
  private final FunctionFormulaManagerView ffmgr;
  private final LogManagerWithoutDuplicates logger;
  private final ShutdownNotifier shutdownNotifier;

  public K3ToFormulaConverter(
      FormulaEncodingOptions pOptions,
      FormulaManagerView pFmgr,
      Optional<VariableClassification> pVariableClassification,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {

    fmgr = pFmgr;
    options = pOptions;
    variableClassification = pVariableClassification;

    bfmgr = fmgr.getBooleanFormulaManager();
    efmgr = fmgr.getBitvectorFormulaManager();
    ffmgr = fmgr.getFunctionFormulaManager();
    logger = new LogManagerWithoutDuplicates(pLogger);
    shutdownNotifier = pShutdownNotifier;
  }

  @FormatMethod
  void logfOnce(Level level, CFAEdge edge, String msg, Object... args) {
    if (logger.wouldBeLogged(level)) {
      logger.logfOnce(
          level,
          "%s: %s: %s",
          edge.getFileLocation(),
          String.format(msg, args),
          edge.getDescription());
    }
  }

  /** Produces a fresh new SSA index for an assignment and updates the SSA map. */
  private int makeFreshIndex(String name, K3Type type, SSAMapBuilder ssa) {
    int idx = getFreshIndex(name, type, ssa);
    ssa.setIndex(name, type, idx);
    return idx;
  }

  /**
   * Produces a fresh new SSA index for an assignment, but does _not_ update the SSA map. Usually
   * you should use {@link #makeFreshIndex(String, K3Type, SSAMapBuilder)} instead, because using
   * variables with indices that are not stored in the SSAMap is not a good idea (c.f. the comment
   * inside getIndex()). If you use this method, you need to make sure to update the SSAMap
   * correctly.
   */
  protected int getFreshIndex(String name, K3Type type, SSAMapBuilder ssa) {
    // TODO: Check that the variable for its type has been declared before?
    // checkSsaSavedType(name, type, ssa.getType(name));
    int idx = ssa.getFreshIndex(name);
    if (idx <= 0) {
      idx = VARIABLE_FIRST_ASSIGNMENT;
    }
    return idx;
  }

  private boolean isRelevantVariable(K3VariableDeclaration pDecl) {
    if (options.ignoreIrrelevantVariables() && variableClassification.isPresent()) {
      return variableClassification
          .orElseThrow()
          .getRelevantVariables()
          .contains(pDecl.getOrigName());
    }
    return true;
  }

  @Override
  public FormulaType<?> getFormulaTypeFromType(Type type) {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  public PathFormula makeAnd(
      PathFormula pOldFormula, CFAEdge pEdge, ErrorConditions pErrorConditions)
      throws UnrecognizedCodeException, InterruptedException {
    String function =
        (pEdge.getPredecessor() != null) ? pEdge.getPredecessor().getFunctionName() : null;

    SSAMapBuilder ssa = pOldFormula.getSsa().builder();
    Constraints constraints = new Constraints(bfmgr);

    BooleanFormula edgeFormula =
        switch (pEdge) {
          case BlankEdge ignored -> bfmgr.makeTrue();
          case K3DeclarationEdge declarationEdge ->
              makeDeclaration(declarationEdge, function, ssa, constraints, pErrorConditions);
          case K3ProcedureCallEdge procedureCallEdge ->
              makeFunctionCall(procedureCallEdge, function, ssa, constraints, pErrorConditions);
          case K3AssumeEdge assumeEdge ->
              makePredicate(assumeEdge, function, ssa, constraints, pErrorConditions);
          case K3ProcedureReturnEdge returnEdge ->
              makeExitProcedure(
                  returnEdge.getSummaryEdge(), function, ssa, constraints, pErrorConditions);

          default -> throw new UnrecognizedCodeException("Unsupported edge", pEdge);
        };

    edgeFormula = bfmgr.and(edgeFormula, constraints.get());
    SSAMap newSsa = ssa.build();

    // There are no pointers in K3, so the pointer target set remains unchanged, and can therefore
    // be ignored.
    if (bfmgr.isTrue(edgeFormula) && (newSsa == pOldFormula.getSsa())) {
      // formula is just "true" and rest is equal
      // i.e. no writes to SSAMap, no branching and length should stay the same
      return pOldFormula;
    }

    BooleanFormula newFormula = bfmgr.and(pOldFormula.getFormula(), edgeFormula);
    int newLength = pOldFormula.getLength() + 1;

    @SuppressWarnings("deprecation")
    // This is an intended use, K3ToFormulaConverter just does not have access to the constructor
    PathFormula result =
        PathFormula.createManually(
            newFormula, newSsa, pOldFormula.getPointerTargetSet(), newLength);
    return result;
  }

  protected BooleanFormula makePredicate(
      K3AssumeEdge edge,
      String function,
      SSAMapBuilder ssa,
      Constraints constraints,
      ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {

    Formula formula = K3TermToFormulaConverter.convertTerm(edge.getExpression(), ssa, fmgr);
    if (!(formula instanceof BooleanFormula booleanFormula)) {
      throw new UnrecognizedCodeException(
          "Expected boolean formula for assume edge, but got: " + formula, edge);
    }

    if (!edge.getTruthAssumption()) {
      booleanFormula = bfmgr.not(booleanFormula);
    }
    return booleanFormula;
  }

  private BooleanFormula makeExitProcedure(
      final K3ProcedureSummaryEdge pEdge,
      final String calledFunction,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {

    K3ProcedureCallStatement procedureCall = pEdge.getExpression();

    // Assign to each of the named function return variables
    // the corresponding return value from the function declaration.
    ImmutableList.Builder<BooleanFormula> assignments = ImmutableList.builder();
    for (int i = 0; i < procedureCall.getReturnVariables().size(); i++) {
      K3IdTerm returnVariableForCall =
          new K3IdTerm(procedureCall.getReturnVariables().get(i), FileLocation.DUMMY);
      K3IdTerm returnVariableFromDeclaration =
          new K3IdTerm(
              procedureCall.getProcedureDeclaration().getReturnValues().get(i), FileLocation.DUMMY);

      assignments.add(
          makeAssignment(
              returnVariableForCall,
              returnVariableForCall,
              returnVariableFromDeclaration,
              pEdge,
              calledFunction,
              ssa,
              constraints,
              errorConditions));
    }

    return bfmgr.and(assignments.build());
  }

  private BooleanFormula makeDeclaration(
      final K3DeclarationEdge edge,
      final String function,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {

    if (!(edge.getDeclaration() instanceof K3VariableDeclaration decl)) {
      // struct prototype, function declaration, typedef etc.
      logfOnce(Level.FINEST, edge, "Ignoring declaration");
      return bfmgr.makeTrue();
    }

    final String varName = decl.getQualifiedName();

    if (!isRelevantVariable(decl)) {
      logger.logfOnce(
          Level.FINEST,
          "%s: Ignoring declaration of unused variable: %s",
          decl.getFileLocation(),
          decl.toASTString());
      return bfmgr.makeTrue();
    }

    makeFreshIndex(varName, decl.getType(), ssa);

    BooleanFormula result = bfmgr.makeTrue();

    // We do not need to handle initializers in K3, since they do not exist.

    return result;
  }

  protected BooleanFormula makeFunctionCall(
      final K3ProcedureCallEdge edge,
      final String callerFunction,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {

    List<K3Term> actualParams = edge.getFunctionCall().getParameterExpressions();

    K3ProcedureEntryNode fn = edge.getSuccessor();
    List<K3ParameterDeclaration> formalParams = fn.getFunctionParameters();

    if (formalParams.size() != actualParams.size()) {
      throw new UnrecognizedCodeException(
          "Number of parameters on function call does not match function definition", edge);
    }

    int i = 0;
    BooleanFormula result = bfmgr.makeTrue();
    for (K3ParameterDeclaration formalParam : formalParams) {
      K3Term paramExpression = actualParams.get(i++);
      K3IdTerm lhs = new K3IdTerm(formalParam, formalParam.getFileLocation());
      final K3IdTerm paramLHS;
      if (options.useParameterVariables()) {
        // make assignments: tmp_param1==arg1, tmp_param2==arg2, ...
        K3ParameterDeclaration tmpParameter =
            new K3ParameterDeclaration(
                formalParam.getFileLocation(),
                formalParam.getType(),
                formalParam.getName(),
                fn.getFunctionName());
        paramLHS = new K3IdTerm(tmpParameter, paramExpression.getFileLocation());
      } else {
        paramLHS = lhs;
      }

      BooleanFormula eq =
          makeAssignment(
              paramLHS,
              lhs,
              paramExpression,
              edge,
              callerFunction,
              ssa,
              constraints,
              errorConditions);
      result = bfmgr.and(result, eq);
    }

    return result;
  }

  protected boolean isRelevantLeftHandSide(final K3IdTerm lhs, final Optional<K3Term> rhs) {
    // TODO: Add for optimizing, based on the one for CtoFormulaConverter
    return true;
  }

  /**
   * Creates formula for the given assignment.
   *
   * @param lhs the left-hand-side of the assignment
   * @param lhsForChecking a left-hand-side of the assignment (for most cases: lhs ==
   *     lhsForChecking), that is used to check, if the assignment is important. If the assignment
   *     is not important, we return TRUE.
   * @param rhs the right-hand-side of the assignment
   * @return the assignment formula
   * @throws InterruptedException may be thrown in subclasses
   */
  protected BooleanFormula makeAssignment(
      final K3IdTerm lhs,
      final K3IdTerm lhsForChecking,
      K3Term rhs,
      final CFAEdge edge,
      final String function,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {

    if (!isRelevantLeftHandSide(lhsForChecking, Optional.of(rhs))) {
      // Optimization for unused variables and fields
      return bfmgr.makeTrue();
    }

    Formula rightHandSideTerm = K3TermToFormulaConverter.convertTerm(rhs, ssa, fmgr);
    Formula assignedVariable =
        makeFreshVariable(lhs.getVariable().getQualifiedName(), lhs.getExpressionType(), ssa);

    return fmgr.assignment(assignedVariable, rightHandSideTerm);
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand side of an
   * assignment. This method does not handle scoping and the NON_DET_VARIABLE!
   */
  protected Formula makeFreshVariable(String name, K3Type type, SSAMapBuilder ssa) {
    int useIndex = makeFreshIndex(name, type, ssa);

    Formula result = fmgr.makeVariable(type.toFormulaType(), name, useIndex);

    return result;
  }

  @Override
  public MergeResult<PointerTargetSet> mergePointerTargetSets(
      PointerTargetSet pPts1, PointerTargetSet pPts2, SSAMapBuilder pNewSSA)
      throws InterruptedException {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  public BooleanFormula makeSsaUpdateTerm(
      String pSymbolName, Type pSymbolType, int pOldIndex, int pNewIndex, PointerTargetSet pOldPts)
      throws InterruptedException {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  public Formula makeFormulaForVariable(
      SSAMap pSsa, PointerTargetSet pPointerTargetSet, String pVarName, CType pType) {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  public Formula makeFormulaForUninstantiatedVariable(
      String pVarName,
      CType pType,
      PointerTargetSet pContextPTS,
      boolean pForcePointerDereference) {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  public Formula buildTermFromPathFormula(PathFormula pFormula, CIdExpression pExpr, CFAEdge pEdge)
      throws UnrecognizedCodeException {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  public void printStatistics(PrintStream pOut) {}
}
