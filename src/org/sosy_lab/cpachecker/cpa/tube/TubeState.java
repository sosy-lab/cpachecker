// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser.Factory;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.InvariantExchangeFormatTransformer;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TubeState implements AbstractQueryableState, Partitionable, Serializable, Graphable,
                                  FormulaReportingState {
  private final ImmutableMap<Integer,String> asserts;
  public LogManager getLogManager() {
    return logManager;
  }
  private final LogManager logManager;
  private final boolean isNegated;
  private final String booleanExp;
  private final CFAEdge cfaEdge;
  private int errorCounter;
  private final CFA cfa;
  public Function<FormulaManagerView, CtoFormulaConverter> getSupplier() {
    return supplier;
  }


  private final Function<FormulaManagerView, CtoFormulaConverter> supplier;
  public TubeState(CFAEdge pCFAEdge, ImmutableMap<Integer, String> pAssert, String exp, boolean pIsNegated, int pError_counter,
                   Function<FormulaManagerView, CtoFormulaConverter> pSupplier, LogManager pLogManager, CFA pCfa){
    this.cfaEdge = pCFAEdge;
    this.asserts = pAssert;
    this.booleanExp = exp;
    this.errorCounter = pError_counter;
    this.isNegated = pIsNegated;
    this.supplier = pSupplier;
    this.logManager = pLogManager;
    this.cfa = pCfa;
  }
  public String getAssertAtLine(int lineNumber, boolean negate){
    String f = this.asserts.get(lineNumber);
    if (negate){
      return "!("+ f + ")";
    }
    return f;
  }
  public CFA getCfa() {
    return cfa;
  }
  public boolean getIsNegated() {return isNegated;}
  public CFAEdge getCfaEdge(){return this.cfaEdge;}
  public ImmutableMap<Integer, String> getAsserts() {
    return this.asserts;
  }
  public String getBooleanExp() {
    return booleanExp;
  }
  public int getErrorCounter(){
    return this.errorCounter;
  }
  public void incrementErrorCounter(){
  this.errorCounter += 1;
}
  @Override
  public String getCPAName() {
    return getClass().getSimpleName();
  }
  @Override
  public @Nullable Object getPartitionKey() {
    return this;
  }
  @Override
  public String toString() {
    return "TubeState{" +
        "asserts=" + asserts +
        ", isNegated=" + isNegated +
        ", booleanExp='" + booleanExp + '\'' +
        ", errorCounter=" + errorCounter +
        '}';
  }
  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    String exp = this.getBooleanExp();
    if (exp == null || !(asserts.containsKey(cfaEdge.getLineNumber()))) {
      return manager.getBooleanFormulaManager().makeTrue();
    }

    BooleanFormula booleanFormula;
    try {
      booleanFormula = manager.uninstantiate(parseFormula(manager, exp,cfaEdge));
    } catch (InvalidAutomatonException pE) {
      throw new RuntimeException(pE);
    } catch (InterruptedException | UnrecognizedCodeException | InvalidConfigurationException pE) {
      throw new RuntimeException(pE);
    }
/*      List<BooleanFormula> booleanFormulas = parseFormula(manager, exp, cfaEdge);
      if (!booleanFormulas.isEmpty()) {
        booleanFormula = manager.uninstantiate(booleanFormulas.get(0));
      }*/
    return Objects.requireNonNullElseGet(booleanFormula,
        manager.getBooleanFormulaManager()::makeTrue);
  }
  @Override
  public String toDOTLabel() {
    return toString();
  }
  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
  BooleanFormula parseFormula(FormulaManagerView pFormulaManagerView, String entry, CFAEdge pCFAEdge)
      throws InvalidAutomatonException, InterruptedException, UnrecognizedCodeException,
             InvalidConfigurationException {
    InvariantExchangeFormatTransformer transformer = new InvariantExchangeFormatTransformer(
        Configuration.defaultConfiguration(),logManager,ShutdownNotifier.createDummy(),cfa);
    Deque<String> callStack = new ArrayDeque<>();
    callStack.push(entry);
    AExpression expression = transformer.createExpressionTreeFromString(Optional.of(entry),entry,cfaEdge.getLineNumber(),
        callStack,new CProgramScope(cfa, logManager)).accept(new ToCExpressionVisitor(cfa.getMachineModel(), logManager));
    String exp = expression.toASTString();
    CStatement statements = CParserUtils.parseSingleStatement(exp, Factory.getParser(logManager, Factory.getDefaultOptions(), cfa.getMachineModel(), ShutdownNotifier.createDummy()),new CProgramScope(cfa, logManager));
    CtoFormulaConverter converter = supplier.apply(pFormulaManagerView);
    CExpression cExpression = getcExpression(statements);
    BooleanFormula
        booleanFormula = converter.makePredicate(cExpression, pCFAEdge, entry, SSAMap.emptySSAMap().builder());
    /*    Set<String> entries = new HashSet<>();
    Deque<String> callStack = new ArrayDeque<>();
    callStack.push(entry);
    ExpressionTree<AExpression> tree = CParserUtils.parseStatementsAsExpressionTree(
        ImmutableSet.of(entry),
        Optional.of(entry),
        Factory.getParser(logManager, Factory.getDefaultOptions(), cfa.getMachineModel(), ShutdownNotifier.createDummy()),
        AutomatonWitnessV2ParserUtils.determineScopeForLine(
            Optional.of(entry), callStack, cfaEdge.getLineNumber(), new CProgramScope(cfa, logManager)),
        ParserTools.create(ExpressionTrees.newFactory(),cfa.getMachineModel(),logManager));
   Collection<CStatement>
        statements = CParserUtils.parseStatements(entries, Optional.ofNullable(entry),
        Factory.getParser(logManager, CParser.Factory.getDefaultOptions(), cfa.getMachineModel(),
            ShutdownNotifier.createDummy()), new CProgramScope(cfa, logManager), ParserTools.create(ExpressionTrees.newFactory(), cfa.getMachineModel(), logManager));
    CtoFormulaConverter converter = supplier.apply(pFormulaManagerView);
    List<CExpression> expressions = getCExpressions(statements);
    List<BooleanFormula> booleanFormulaList = new ArrayList<>();
    for (CExpression expression : expressions) {
      booleanFormulaList.add(
          converter.makePredicate(expression, pCFAEdge, entry, SSAMap.emptySSAMap().builder()));*/
    return booleanFormula;
    }


/*  private static List<CExpression> getCExpressions(Collection<CStatement> statements) {
    return statements.stream().map(statement -> ((CExpressionStatement) statement).getExpression())
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }*/
  private static CExpression getcExpression(CStatement statement) {
    CExpression expression = ((CExpressionStatement) statement).getExpression();
    if (expression == null) {
      throw new IllegalArgumentException(
          "Statement cannot be converted into CExpression. Invalid statement: " + statement);
    }
    return expression;




}
}
