// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.Pair;

public class ParseResultWithCommentLocations extends ParseResult {

  private List<FileLocation> commentLocations;
  private Map<CFANode, Integer> statementStackDepths;

  public ParseResultWithCommentLocations(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pCfaNodes,
      List<Pair<ADeclaration, String>> pGlobalDeclarations,
      List<Path> pFileNames,
      List<FileLocation> pCommentLocations,
      Map<CFANode, Integer> pStatementStackDepths) {
    super(pFunctions, pCfaNodes, pGlobalDeclarations, pFileNames);
    commentLocations = pCommentLocations;
    statementStackDepths = pStatementStackDepths;
  }

  public List<FileLocation> getCommentLocations() {
    return commentLocations;
  }

  public Map<CFANode, Integer> getStatementStackDepths() {
    return statementStackDepths;
  }
}
