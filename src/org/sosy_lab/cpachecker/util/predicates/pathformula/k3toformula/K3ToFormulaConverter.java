// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.k3toformula;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.k3toformula.K3ToSmtConverterUtils.cleanVariableNameForJavaSMT;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.FormatMethod;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.IntStream;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssignmentStatement;
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
import org.sosy_lab.cpachecker.cfa.model.k3.K3StatementEdge;
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
              makeProcedureCall(procedureCallEdge, function, ssa, constraints, pErrorConditions);
          case K3AssumeEdge assumeEdge ->
              makePredicate(assumeEdge, function, ssa, constraints, pErrorConditions);
          case K3ProcedureReturnEdge returnEdge ->
              makeExitProcedure(
                  returnEdge.getSummaryEdge(), function, ssa, constraints, pErrorConditions);
          case K3StatementEdge statementEdge ->
              K3StatementToFormulaConverter.convertStatement(
                  statementEdge.getStatement(), ssa, fmgr);

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
      final ErrorConditions errorConditions) {

    K3ProcedureCallStatement procedureCall = pEdge.getExpression();

    return K3StatementToFormulaConverter.convertStatement(
        new K3AssignmentStatement(
            zipToMap(
                procedureCall.getReturnVariables(),
                transformedImmutableListCopy(
                    procedureCall.getProcedureDeclaration().getReturnValues(),
                    var -> new K3IdTerm(var, FileLocation.DUMMY))),
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of()),
        ssa,
        fmgr);
  }

  private BooleanFormula makeDeclaration(
      final K3DeclarationEdge edge,
      final String function,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final ErrorConditions errorConditions) {

    if (!(edge.getDeclaration() instanceof K3VariableDeclaration decl)) {
      // struct prototype, function declaration, typedef etc.
      logfOnce(Level.FINEST, edge, "Ignoring declaration");
      return bfmgr.makeTrue();
    }

    final String varName = cleanVariableNameForJavaSMT(decl.getQualifiedName());

    if (!isRelevantVariable(decl)) {
      logger.logfOnce(
          Level.FINEST,
          "%s: Ignoring declaration of unused variable: %s",
          decl.getFileLocation(),
          decl.toASTString());
      return bfmgr.makeTrue();
    }

    SSAHandler.makeFreshIndex(varName, decl.getType(), ssa);

    BooleanFormula result = bfmgr.makeTrue();

    // We do not need to handle initializers in K3, since they do not exist.

    return result;
  }

  public static <K, V> Map<K, V> zipToMap(List<? extends K> keys, List<? extends V> values) {
    return IntStream.range(0, keys.size())
        .boxed()
        .collect(ImmutableMap.toImmutableMap(keys::get, values::get));
  }

  protected BooleanFormula makeProcedureCall(
      final K3ProcedureCallEdge edge,
      final String callerFunction,
      final SSAMapBuilder ssa,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCodeException {

    List<K3Term> actualParams = edge.getFunctionCall().getParameterExpressions();

    K3ProcedureEntryNode fn = edge.getSuccessor();
    List<K3ParameterDeclaration> formalParams = fn.getFunctionParameters();

    if (formalParams.size() != actualParams.size()) {
      throw new UnrecognizedCodeException(
          "Number of parameters on function call does not match function definition", edge);
    }

    // Initialize return variables to default values (nondet)
    for (K3ParameterDeclaration returnVarDecl :
        edge.getFunctionCall().getProcedureDeclaration().getReturnValues()) {
      SSAHandler.makeFreshIndex(
          cleanVariableNameForJavaSMT(returnVarDecl.getQualifiedName()),
          returnVarDecl.getType(),
          ssa);
    }

    // Initialize local variables to default values (nondet)
    for (K3ParameterDeclaration localVarDecl :
        edge.getFunctionCall().getProcedureDeclaration().getLocalVariables()) {
      SSAHandler.makeFreshIndex(
          cleanVariableNameForJavaSMT(localVarDecl.getQualifiedName()),
          localVarDecl.getType(),
          ssa);
    }

    return K3StatementToFormulaConverter.convertStatement(
        new K3AssignmentStatement(
            zipToMap(formalParams, actualParams),
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of()),
        ssa,
        fmgr);
  }

  protected boolean isRelevantLeftHandSide(final K3IdTerm lhs, final Optional<K3Term> rhs) {
    // TODO: Add for optimizing, based on the one for CtoFormulaConverter
    return true;
  }

  @Override
  public MergeResult<PointerTargetSet> mergePointerTargetSets(
      PointerTargetSet pPts1, PointerTargetSet pPts2, SSAMapBuilder pNewSSA)
      throws InterruptedException {
    // They should always be equal, as there are no pointers in K3.
    Verify.verify(pPts1.equals(pPts2));
    return MergeResult.trivial(pPts1, fmgr.getBooleanFormulaManager());
  }

  @Override
  public BooleanFormula makeSsaUpdateTerm(
      String pSymbolName, Type pSymbolType, int pOldIndex, int pNewIndex, PointerTargetSet pOldPts)
      throws InterruptedException {
    checkArgument(pOldIndex > 0 && pNewIndex > pOldIndex);
    checkArgument(pSymbolType instanceof K3Type);

    final FormulaType<?> variableFormulaType = ((K3Type) pSymbolType).toFormulaType();
    final Formula oldVariable = fmgr.makeVariable(variableFormulaType, pSymbolName, pOldIndex);
    final Formula newVariable = fmgr.makeVariable(variableFormulaType, pSymbolName, pNewIndex);
    return fmgr.assignment(newVariable, oldVariable);
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
