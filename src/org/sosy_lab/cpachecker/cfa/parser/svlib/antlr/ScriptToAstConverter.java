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
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibParsingResult;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser.ScriptContext;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibScript;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;

class ScriptToAstConverter extends AbstractAntlrToAstConverter<SvLibParsingResult> {

  private final CommandToAstConverter commandToAstConverter;
  private final ImmutableMap.Builder<SvLibTagReference, SvLibScope> tagReferenceToScopeBuilder;

  public ScriptToAstConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    tagReferenceToScopeBuilder = ImmutableMap.builder();
    commandToAstConverter =
        new CommandToAstConverter(pScope, pFilePath, tagReferenceToScopeBuilder);
  }

  public ScriptToAstConverter(SvLibScope pScope) {
    super(pScope);
    tagReferenceToScopeBuilder = ImmutableMap.builder();
    commandToAstConverter = new CommandToAstConverter(pScope, tagReferenceToScopeBuilder);
  }

  @Override
  public SvLibParsingResult visitScript(ScriptContext ctx) {
    ImmutableList.Builder<SvLibCommand> commands = ImmutableList.builder();
    for (var commandCtx : ctx.commandSvLib()) {
      SvLibCommand command = commandToAstConverter.visit(commandCtx);
      commands.add(command);
    }
    return new SvLibParsingResult(
        new SvLibScript(commands.build(), fileLocationFromContext(ctx)),
        tagReferenceToScopeBuilder.buildOrThrow());
  }
}
