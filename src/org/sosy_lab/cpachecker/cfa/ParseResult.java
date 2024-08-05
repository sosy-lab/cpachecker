// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeMultimap;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableMap;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Class representing the result of parsing a C file before function calls are bound to their
 * targets.
 *
 * <p>It consists of a map that stores the CFAs for each function and a list of declarations of
 * global variables.
 *
 * <p>This class is immutable, but it does not ensure that it's content also is. It is recommended
 * to use it only as a "transport" data class, not for permanent storage.
 */
public class ParseResult {

  private final NavigableMap<String, FunctionEntryNode> functions;

  private final TreeMultimap<String, CFANode> cfaNodes;

  private final List<Pair<ADeclaration, String>> globalDeclarations;

  private final List<Path> fileNames;

  public ParseResult(
      NavigableMap<String, FunctionEntryNode> pFunctions,
      TreeMultimap<String, CFANode> pCfaNodes,
      List<Pair<ADeclaration, String>> pGlobalDeclarations,
      List<Path> pFileNames) {
    functions = pFunctions;
    cfaNodes = pCfaNodes;
    globalDeclarations = pGlobalDeclarations;
    fileNames = ImmutableList.copyOf(pFileNames);
  }

  public boolean isEmpty() {
    return functions.isEmpty();
  }

  public NavigableMap<String, FunctionEntryNode> getFunctions() {
    return functions;
  }

  public TreeMultimap<String, CFANode> getCFANodes() {
    return cfaNodes;
  }

  public List<Pair<ADeclaration, String>> getGlobalDeclarations() {
    return globalDeclarations;
  }

  public List<Path> getFileNames() {
    return fileNames;
  }
}
