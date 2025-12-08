// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AnnotateTagCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.Cmd_setInfoContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.CommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.CorrectnessWitnessContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.SelectTraceCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ViolationWitnessContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibCorrectnessWitness;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibViolationWitness;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibWitness;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSelectTraceCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;

class WitnessToAstConverter extends AbstractAntlrToAstConverter<SvLibWitness> {

  private final SvLibCommandToAstConverter commandToAstConverter;
  private final Builder<SvLibTagReference, SvLibScope> tagReferenceToScopeBuilder;

  public WitnessToAstConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    tagReferenceToScopeBuilder = ImmutableMap.builder();
    commandToAstConverter =
        new SvLibCommandToAstConverter(pScope, pFilePath, tagReferenceToScopeBuilder);
  }

  public WitnessToAstConverter(SvLibScope pScope) {
    super(pScope);
    tagReferenceToScopeBuilder = ImmutableMap.builder();
    commandToAstConverter = new SvLibCommandToAstConverter(pScope, tagReferenceToScopeBuilder);
  }

  @Override
  public SvLibCorrectnessWitness visitCorrectnessWitness(CorrectnessWitnessContext ctx) {
    ImmutableList.Builder<SmtLibCommand> smtLibCommandBuilder = ImmutableList.builder();
    ImmutableList.Builder<SvLibSetInfoCommand> setInfoCommandBuilder = ImmutableList.builder();

    // Parse the SMT-LIB commands in the correctness witness dividing them into
    // metadata and others
    for (Cmd_setInfoContext commandCtx : ctx.metadata().cmd_setInfo()) {
      SvLibCommand command = commandToAstConverter.visit(commandCtx);
      if (command instanceof SvLibSetInfoCommand pSetInfoCommand) {
        setInfoCommandBuilder.add(pSetInfoCommand);
      } else {
        throw new IllegalArgumentException(
            "Expected set-info command in metadata of correctness witness, but got: "
                + command.getClass().getName());
      }
    }

    for (CommandContext commandCtx : ctx.command()) {
      SvLibCommand command = commandToAstConverter.visit(commandCtx);
      if (command instanceof SvLibSetInfoCommand pSetInfoCommand) {
        setInfoCommandBuilder.add(pSetInfoCommand);
      } else if (command instanceof SmtLibCommand pSmtLibCommand) {
        smtLibCommandBuilder.add(pSmtLibCommand);
      } else {
        throw new IllegalArgumentException(
            "Expected SmtLibCommand in correctness witness, but got: "
                + command.getClass().getName());
      }
    }

    // Now parse the annotate-tag commands
    ImmutableList.Builder<SvLibAnnotateTagCommand> annotateTagCommandBuilder =
        ImmutableList.builder();
    for (AnnotateTagCommandContext annotateTagCtx : ctx.annotateTagCommand()) {
      SvLibCommand command = commandToAstConverter.visit(annotateTagCtx);
      if (command instanceof SvLibAnnotateTagCommand pAnnotateTagCommand) {
        annotateTagCommandBuilder.add(pAnnotateTagCommand);
      } else {
        throw new IllegalArgumentException(
            "Expected SvLibAnnotateTagCommand in correctness witness, but got: "
                + command.getClass().getName());
      }
    }

    // Build and return the correctness witness AST node
    return new SvLibCorrectnessWitness(
        fileLocationFromContext(ctx),
        setInfoCommandBuilder.build(),
        smtLibCommandBuilder.build(),
        annotateTagCommandBuilder.build());
  }

  @Override
  public SvLibViolationWitness visitViolationWitness(ViolationWitnessContext ctx) {
    ImmutableList.Builder<SvLibSetInfoCommand> setInfoCommandBuilder = ImmutableList.builder();

    // First parse the SMT-LIB commands in the violation witness
    for (Cmd_setInfoContext commandCtx : ctx.metadata().cmd_setInfo()) {
      SvLibCommand command = commandToAstConverter.visit(commandCtx);
      if (command instanceof SvLibSetInfoCommand pSetInfoCommand) {
        setInfoCommandBuilder.add(pSetInfoCommand);
      } else {
        throw new IllegalArgumentException(
            "Expected set-info command in metadata of violation witness, but got: "
                + command.getClass().getName());
      }
    }

    // Now parse the select-trace commands
    ImmutableList.Builder<SvLibSelectTraceCommand> selectTraceCommandBuilder =
        ImmutableList.builder();
    for (SelectTraceCommandContext commandCtx : ctx.selectTraceCommand()) {
      SvLibCommand command = commandToAstConverter.visit(commandCtx);
      if (command instanceof SvLibSelectTraceCommand pSvLibCommand) {
        selectTraceCommandBuilder.add(pSvLibCommand);
      } else {
        throw new IllegalArgumentException(
            "Expected a select-trace command in the violation witness, but got: "
                + command.getClass().getName());
      }
    }

    return new SvLibViolationWitness(
        fileLocationFromContext(ctx),
        setInfoCommandBuilder.build(),
        selectTraceCommandBuilder.build());
  }
}
