// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

public class CFAToCExporter {

  private static final boolean NAMES_QUALIFIED = false;

  /**
   * Exports the given {@link CFA} to a C program.
   *
   * @param pCfa the CFA to export
   * @return C representation of the given CFA
   * @throws InvalidConfigurationException if the given CFA is not the CFA of a C program
   */
  public String exportCfa(CFA pCfa) throws InvalidConfigurationException {
    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be exported to C for C input programs, at the moment");
    }

    StringBuilder buffer = new StringBuilder();

    for (FunctionEntryNode functionEntryNode : pCfa.getAllFunctionHeads()) {
      buffer.append(
          functionEntryNode
              .getFunctionDefinition()
              .toASTString(NAMES_QUALIFIED)
              .replace(";", " {\n"));
      // TODO
      buffer.append("}\n");
    }

    return buffer.toString();
  }
}
