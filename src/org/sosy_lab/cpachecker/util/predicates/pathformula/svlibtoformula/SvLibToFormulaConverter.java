// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula.SvLibToSmtConverterUtils.cleanVariableNameForJavaSMT;

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
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibProcedureCallEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibProcedureEntryNode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibProcedureReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibProcedureSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.LanguageToSmtConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

// TODO: Figure out if we actually need all the paramters which are currently being suppressed
//      as unused.
public class SvLibToFormulaConverter extends LanguageToSmtConverter {

  private final FormulaManagerView fmgr;
  private final SvLibFormulaEncodingOptions options;
  private final Optional<VariableClassification> variableClassification;
  private final BooleanFormulaManagerView bfmgr;

  @SuppressWarnings("unused")
  private final BitvectorFormulaManagerView efmgr;

  @SuppressWarnings("unused")
  private final FunctionFormulaManagerView ffmgr;

  private final LogManagerWithoutDuplicates logger;

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  public SvLibToFormulaConverter(
      SvLibFormulaEncodingOptions pOptions,
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

  private boolean isRelevantVariable(SvLibVariableDeclaration pDecl) {
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
          case SvLibDeclarationEdge declarationEdge ->
              makeDeclaration(declarationEdge, function, ssa, constraints, pErrorConditions);
          case SvLibProcedureCallEdge procedureCallEdge ->
              makeProcedureCall(procedureCallEdge, function, ssa, constraints, pErrorConditions);
          case SvLibAssumeEdge assumeEdge ->
              makePredicate(assumeEdge, function, ssa, constraints, pErrorConditions);
          case SvLibProcedureReturnEdge returnEdge ->
              makeExitProcedure(
                  returnEdge.getSummaryEdge(), function, ssa, constraints, pErrorConditions);
          case SvLibStatementEdge statementEdge ->
              SvLibStatementToFormulaConverter.convertStatement(
                  statementEdge.getStatement(), ssa, fmgr);

          default -> throw new UnrecognizedCodeException("Unsupported edge", pEdge);
        };

    edgeFormula = bfmgr.and(edgeFormula, constraints.get());
    SSAMap newSsa = ssa.build();

    // There are no pointers in SV-LIB, so the pointer target set remains unchanged, and can
    // therefore
    // be ignored.
    if (bfmgr.isTrue(edgeFormula) && (newSsa == pOldFormula.getSsa())) {
      // formula is just "true" and rest is equal
      // i.e. no writes to SSAMap, no branching and length should stay the same
      return pOldFormula;
    }

    BooleanFormula newFormula = bfmgr.and(pOldFormula.getFormula(), edgeFormula);
    int newLength = pOldFormula.getLength() + 1;

    @SuppressWarnings("deprecation")
    // This is an intended use, SvLibToFormulaConverter just does not have access to the constructor
    PathFormula result =
        PathFormula.createManually(
            newFormula, newSsa, pOldFormula.getPointerTargetSet(), newLength);
    return result;
  }

  protected BooleanFormula makePredicate(
      SvLibAssumeEdge edge,
      @SuppressWarnings("unused") String function,
      SSAMapBuilder ssa,
      @SuppressWarnings("unused") Constraints constraints,
      @SuppressWarnings("unused") ErrorConditions errorConditions)
      throws UnrecognizedCodeException {

    Formula formula = SvLibTermToFormulaConverter.convertTerm(edge.getExpression(), ssa, fmgr);
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
      final SvLibProcedureSummaryEdge pEdge,
      @SuppressWarnings("unused") final String calledFunction,
      final SSAMapBuilder ssa,
      @SuppressWarnings("unused") final Constraints constraints,
      @SuppressWarnings("unused") final ErrorConditions errorConditions) {

    SvLibProcedureCallStatement procedureCall = pEdge.getExpression();

    return SvLibStatementToFormulaConverter.convertStatement(
        new SvLibAssignmentStatement(
            zipToMap(
                procedureCall.getReturnVariables(),
                transformedImmutableListCopy(
                    procedureCall.getProcedureDeclaration().getReturnValues(),
                    var -> new SvLibIdTerm(var, FileLocation.DUMMY))),
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of()),
        ssa,
        fmgr);
  }

  private BooleanFormula makeDeclaration(
      final SvLibDeclarationEdge edge,
      @SuppressWarnings("unused") final String function,
      final SSAMapBuilder ssa,
      @SuppressWarnings("unused") final Constraints constraints,
      @SuppressWarnings("unused") final ErrorConditions errorConditions) {

    if (!(edge.getDeclaration() instanceof SvLibVariableDeclaration decl)) {
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

    LanguageToSmtConverter.makeFreshIndex(varName, decl.getType(), ssa);

    BooleanFormula result = bfmgr.makeTrue();

    // We do not need to handle initializers in SV-LIB, since they do not exist.

    return result;
  }

  public static <K, V> Map<K, V> zipToMap(List<? extends K> keys, List<? extends V> values) {
    return IntStream.range(0, keys.size())
        .boxed()
        .collect(ImmutableMap.toImmutableMap(keys::get, values::get));
  }

  protected BooleanFormula makeProcedureCall(
      final SvLibProcedureCallEdge edge,
      @SuppressWarnings("unused") final String callerFunction,
      final SSAMapBuilder ssa,
      @SuppressWarnings("unused") final Constraints constraints,
      @SuppressWarnings("unused") final ErrorConditions errorConditions)
      throws UnrecognizedCodeException {

    List<SvLibTerm> actualParams =
        (List<SvLibTerm>) edge.getFunctionCallExpression().getParameterExpressions();

    SvLibProcedureEntryNode fn = edge.getSuccessor();
    List<SvLibParameterDeclaration> formalParams = fn.getFunctionParameters();

    if (formalParams.size() != actualParams.size()) {
      throw new UnrecognizedCodeException(
          "Number of parameters on function call does not match function definition", edge);
    }

    // Initialize return variables to default values (nondet)
    for (SvLibParameterDeclaration returnVarDecl :
        edge.getFunctionCall().getProcedureDeclaration().getReturnValues()) {
      LanguageToSmtConverter.makeFreshIndex(
          cleanVariableNameForJavaSMT(returnVarDecl.getQualifiedName()),
          returnVarDecl.getType(),
          ssa);
    }

    // Initialize local variables to default values (nondet)
    for (SvLibParameterDeclaration localVarDecl :
        edge.getFunctionCall().getProcedureDeclaration().getLocalVariables()) {
      LanguageToSmtConverter.makeFreshIndex(
          cleanVariableNameForJavaSMT(localVarDecl.getQualifiedName()),
          localVarDecl.getType(),
          ssa);
    }

    return SvLibStatementToFormulaConverter.convertStatement(
        new SvLibAssignmentStatement(
            zipToMap(formalParams, actualParams),
            FileLocation.DUMMY,
            ImmutableList.of(),
            ImmutableList.of()),
        ssa,
        fmgr);
  }

  @SuppressWarnings("unused")
  protected boolean isRelevantLeftHandSide(final SvLibIdTerm lhs, final Optional<SvLibTerm> rhs) {
    // TODO: Add for optimizing, based on the one for CtoFormulaConverter
    return true;
  }

  @Override
  public MergeResult<PointerTargetSet> mergePointerTargetSets(
      PointerTargetSet pPts1, PointerTargetSet pPts2, SSAMapBuilder pNewSSA)
      throws InterruptedException {
    // They should always be equal, as there are no pointers in SV-LIB.
    Verify.verify(pPts1.equals(pPts2));
    return MergeResult.trivial(pPts1, fmgr.getBooleanFormulaManager());
  }

  @Override
  public BooleanFormula makeSsaUpdateTerm(
      String pSymbolName, Type pSymbolType, int pOldIndex, int pNewIndex, PointerTargetSet pOldPts)
      throws InterruptedException {
    checkArgument(pOldIndex > 0 && pNewIndex > pOldIndex);
    checkArgument(pSymbolType instanceof SvLibType);

    final FormulaType<?> variableFormulaType = ((SvLibType) pSymbolType).toFormulaType();
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
