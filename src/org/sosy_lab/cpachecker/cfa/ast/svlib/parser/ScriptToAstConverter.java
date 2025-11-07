// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.parser;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibScript;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser.ScriptContext;

class ScriptToAstConverter extends AbstractAntlrToAstConverter<SvLibScript> {

  private final CommandToAstConverter commandToAstConverter;

  public ScriptToAstConverter(SvLibScope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    commandToAstConverter = new CommandToAstConverter(pScope, pFilePath);
  }

  public ScriptToAstConverter(SvLibScope pScope) {
    super(pScope);
    commandToAstConverter = new CommandToAstConverter(pScope);
  }

  @Override
  public SvLibScript visitScript(ScriptContext ctx) {
    ImmutableList.Builder<SvLibCommand> commands = ImmutableList.builder();
    for (var commandCtx : ctx.commandSvLib()) {
      SvLibCommand command = commandToAstConverter.visit(commandCtx);
      commands.add(command);
    }
    return new SvLibScript(commands.build());
  }
}
