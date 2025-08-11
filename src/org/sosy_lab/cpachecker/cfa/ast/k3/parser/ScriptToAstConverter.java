// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Command;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Script;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser.ScriptContext;

public class ScriptToAstConverter extends AbstractAntlrToAstConverter<K3Script> {

  private final CommandToAstConverter commandToAstConverter;

  public ScriptToAstConverter(K3Scope pScope, Path pFilePath) {
    super(pScope, pFilePath);
    commandToAstConverter = new CommandToAstConverter(pScope, pFilePath);
  }

  public ScriptToAstConverter(K3Scope pScope) {
    super(pScope);
    commandToAstConverter = new CommandToAstConverter(pScope);
  }

  @Override
  public K3Script visitScript(ScriptContext ctx) {
    ImmutableList.Builder<K3Command> commands = ImmutableList.builder();
    for (var commandCtx : ctx.command()) {
      K3Command command = commandToAstConverter.visit(commandCtx);
      commands.add(command);
    }
    return new K3Script(commands.build());
  }
}
