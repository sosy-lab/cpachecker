// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ChooseChoiceStatementContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ChooseHavocVariableValueContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ChooseLocalVariableValueContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.LeapStepContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.TraceVariableAssignmentContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibChoiceStep;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibHavocVariablesStep;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibInitProcVariablesStep;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibLeapStep;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.trace.SvLibTraceStep;

public class SvLibStepToAstConverter extends AbstractAntlrToAstConverter<SvLibTraceStep> {
  private final TermToAstConverter termConverter;

  public SvLibStepToAstConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    termConverter = new TermToAstConverter(pScope, pFilePath);
  }

  public SvLibStepToAstConverter(SvLibScope pScope) {
    super(pScope);
    termConverter = new TermToAstConverter(pScope);
  }

  private ImmutableMap<SvLibIdTerm, SvLibConstantTerm> convertVariableAssignments(
      Iterable<TraceVariableAssignmentContext> pAssignments) {
    ImmutableMap.Builder<SvLibIdTerm, SvLibConstantTerm> varAssignmentsBuilder =
        ImmutableMap.builder();
    for (TraceVariableAssignmentContext assignmentContext : pAssignments) {
      SvLibIdTerm symbol = (SvLibIdTerm) termConverter.visit(assignmentContext.symbol());
      SvLibConstantTerm constant =
          (SvLibConstantTerm) termConverter.visit(assignmentContext.spec_constant());
      varAssignmentsBuilder.put(symbol, constant);
    }
    return varAssignmentsBuilder.buildOrThrow();
  }

  @Override
  public SvLibInitProcVariablesStep visitChooseLocalVariableValue(
      ChooseLocalVariableValueContext pContext) {
    SvLibProcedureDeclaration procDecl = scope.getProcedureDeclaration(pContext.symbol().getText());
    ImmutableMap<SvLibIdTerm, SvLibConstantTerm> varAssignments =
        convertVariableAssignments(pContext.traceVariableAssignment());

    return new SvLibInitProcVariablesStep(
        procDecl, varAssignments, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibHavocVariablesStep visitChooseHavocVariableValue(
      ChooseHavocVariableValueContext pContext) {
    ImmutableMap<SvLibIdTerm, SvLibConstantTerm> varAssignments =
        convertVariableAssignments(pContext.traceVariableAssignment());

    return new SvLibHavocVariablesStep(varAssignments, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibChoiceStep visitChooseChoiceStatement(ChooseChoiceStatementContext pContext) {
    final int choice;
    try {
      choice = Integer.parseInt(pContext.Numeral().getText());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "The choice value %s is not a valid integer.".formatted(pContext.Numeral().getText()), e);
    }

    return new SvLibChoiceStep(choice, fileLocationFromContext(pContext));
  }

  @Override
  public SvLibTraceStep visitLeapStep(LeapStepContext pContext) {
    ImmutableMap<SvLibIdTerm, SvLibConstantTerm> varAssignments =
        convertVariableAssignments(pContext.traceVariableAssignment());
    return new SvLibLeapStep(
        fileLocationFromContext(pContext), varAssignments, pContext.symbol().getText());
  }
}
