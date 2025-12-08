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
import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.AnnotateTagCommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.CommandContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.CorrectnessWitnessContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibCorrectnessWitness;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibWitness;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetInfoCommand;

class WitnessToAstConverter extends AbstractAntlrToAstConverter<SvLibWitness> {

  private final SvLibCommandToAstConverter commandToAstConverter;
  private final ImmutableMap.Builder<SvLibTagReference, SvLibScope> tagReferenceToScopeBuilder;

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
}
