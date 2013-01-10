/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * This is a parser for files which contain initial predicates for the analysis.
 * The file format is based on SMTLIB2 and defines a map from location to
 * sets of predicates.
 * The keys of the map (the locations) may either be CFA nodes (N1 etc.),
 * function names of the analyzed program (which stand for all locations in the respective function),
 * or the special identifier "*" (which stands for all locations in the program).
 *
 * Detailed format description:
 *
 * - Lines starting with "//" are comments.
 * - Blank lines separates sections in the file.
 * - The first section (starting in the first line) consists of an arbitrary
 *   number of lines of the format "(declare-fun ...)" or "(define-fun ...)"
 *   with definitions in SMTLIB2 format.
 * - Every section except the first one starts with a line of the format "key:",
 *   where key is either "*", a function name of the program, or a node of the CFA
 *   (identified by "Nid", where id is the node number).
 *   This line defines where the following predicates are to be used.
 * - The following lines of the section contain SMTLIB2 statements of the form
 *   "(assert ...)". Each asserted term will be used as one predicate.
 */
public class PredicateMapParser {

  private static final Pattern CFA_NODE_PATTERN = Pattern.compile("^N[0-9][0-9]*");
  private static final Pattern FUNCTION_NAME_PATTERN = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*$");

  public static class PredicateMapParsingFailedException extends Exception {
    private static final long serialVersionUID = 5034288100943314517L;

    private PredicateMapParsingFailedException(String msg, String source, int lineNo) {
      super("Parsing failed in line " + lineNo + " of " + source + ": " + msg);
    }


    private PredicateMapParsingFailedException(Throwable cause, String source, int lineNo) {
      this(cause.getMessage(), source, lineNo);
      initCause(cause);
    }
  }

  /**
   * Parse a file in the format described above and create a PredicatePrecision
   * object with all the predicates.
   * @param file The file to parse.
   * @param cfa The CFA.
   * @param initialGlobalPredicates A additional set of global predicates (for all locations).
   * @param fmgr The FormulaManager which is used to parse the predicates.
   * @param amgr The AbstractionManager which is used to create the AbstractionPredicates.
   * @return A PredicatePrecision containing all the predicates from the file.
   * @throws IOException If the file cannot be read.
   * @throws PredicateMapParsingFailedException If there is a syntax error in the file.
   */
  public static PredicatePrecision parsePredicates(File file, CFA cfa,
      Collection<AbstractionPredicate> initialGlobalPredicates,
      FormulaManager fmgr, AbstractionManager amgr)
          throws IOException, PredicateMapParsingFailedException {

    Files.checkReadableFile(file);

    try (BufferedReader reader = java.nio.file.Files.newBufferedReader(file.toPath(), Charsets.US_ASCII)) {
      return parsePredicates(reader, file.getName(),
          cfa, initialGlobalPredicates, fmgr, amgr);
    }
  }

  /**
   * @see #parsePredicates(File, CFA, Collection, FormulaManager, AbstractionManager, LogManager)
   * Instead of reading from a file, this method reads from a BufferedReader
   * (available primarily for testing).
   */
  static PredicatePrecision parsePredicates(BufferedReader reader, String source,
      CFA cfa, Collection<AbstractionPredicate> initialGlobalPredicates,
      FormulaManager fmgr, AbstractionManager amgr)
          throws IOException, PredicateMapParsingFailedException {

    // first, read first section with initial set of function definitions
    StringBuilder functionDefinitionsBuffer = new StringBuilder();

    int lineNo = 0;
    String currentLine;
    while ((currentLine = reader.readLine()) != null) {
      lineNo++;
      currentLine = currentLine.trim();

      if (currentLine.isEmpty()) {
        break;
      }

      if (currentLine.startsWith("//")) {
        // comment
        continue;
      }

      if (currentLine.startsWith("(") && currentLine.endsWith(")")) {
        functionDefinitionsBuffer.append(currentLine);
        functionDefinitionsBuffer.append('\n');

      } else {
        throw new PredicateMapParsingFailedException(currentLine + " is not a valid SMTLIB2 definition", source, lineNo);
      }
    }
    String functionDefinitions = functionDefinitionsBuffer.toString();

    // second, read map of predicates
    Set<AbstractionPredicate> globalPredicates = Sets.newHashSet(initialGlobalPredicates);
    SetMultimap<String, AbstractionPredicate> functionPredicates = HashMultimap.create();
    SetMultimap<CFANode, AbstractionPredicate> localPredicates = HashMultimap.create();

    Map<Integer, CFANode> idToNodeMap = null;

    Set<AbstractionPredicate> currentSet = null;
    while ((currentLine = reader.readLine()) != null) {
      lineNo++;
      currentLine = currentLine.trim();

      if (currentLine.isEmpty()) {
        // blank lines separates sections
        currentSet = null;
        continue;
      }

      if (currentLine.startsWith("//")) {
        // comment
        continue;
      }

      if (currentSet == null) {
        // we expect a new section header
        if (!currentLine.endsWith(":")) {
          throw new PredicateMapParsingFailedException(currentLine + " is not a valid section header", source, lineNo);
        }
        currentLine = currentLine.substring(0, currentLine.length()-1).trim(); // strip off ":"
        if (currentLine.isEmpty()) {
          throw new PredicateMapParsingFailedException("empty key is not allowed", source, lineNo);
        }

        if (currentLine.equals("*")) {
          // the section "*"
          currentSet = globalPredicates;

        } else if (CFA_NODE_PATTERN.matcher(currentLine).matches()) {
          // a section with a CFA node

          if (idToNodeMap == null) {
            idToNodeMap = createMappingForCFANodes(cfa);
          }
          currentLine = currentLine.substring(1); // strip off "N"
          int nodeId = Integer.parseInt(currentLine); // does not fail, we checked with regexp
          CFANode node = idToNodeMap.get(nodeId);
          currentSet = localPredicates.get(node);

        } else if (FUNCTION_NAME_PATTERN.matcher(currentLine).matches()) {
          // a section with a function name

          if (!cfa.getAllFunctionNames().contains(currentLine)) {
            throw new PredicateMapParsingFailedException(currentLine + " is an unknown function", source, lineNo);
          }

          currentSet = functionPredicates.get(currentLine);

        } else {
          throw new PredicateMapParsingFailedException(currentLine + " is not a valid key", source, lineNo);
        }

      } else {
        // we expect a predicate
        if (currentLine.startsWith("(assert ") && currentLine.endsWith(")")) {
          BooleanFormula f;
          try {
            f = fmgr.parse(BooleanFormula.class, functionDefinitions + currentLine);
          } catch (IllegalArgumentException e) {
            throw new PredicateMapParsingFailedException(e, source, lineNo);
          }

          currentSet.add(amgr.makePredicate(f));

        } else {
          throw new PredicateMapParsingFailedException("unexpected line " + currentLine, source, lineNo);
        }
      }
    }

    return new PredicatePrecision(localPredicates, functionPredicates, globalPredicates);
  }

  private static Map<Integer, CFANode> createMappingForCFANodes(CFA cfa) {
    Map<Integer, CFANode> idToNodeMap = Maps.newHashMap();
    for (CFANode n : cfa.getAllNodes()) {
      idToNodeMap.put(n.getNodeNumber(), n);
    }
    return idToNodeMap;
  }
}
