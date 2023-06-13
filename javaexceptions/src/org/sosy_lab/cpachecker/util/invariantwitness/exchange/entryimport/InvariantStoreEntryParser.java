// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.entryimport;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessFactory;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantStoreUtil;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;

class InvariantStoreEntryParser {
  private final ListMultimap<String, Integer> lineOffsetsByFile;
  private final LogManager logger;
  private final InvariantWitnessFactory invariantWitnessFactory;
  private final CParser parser;
  private final CProgramScope scope;
  private final ParserTools parserTools;
  private final CFA cfa;

  private InvariantStoreEntryParser(
      ListMultimap<String, Integer> pLineOffsetsByFile,
      LogManager pLogger,
      InvariantWitnessFactory pInvariantWitnessFactory,
      CParser pParser,
      CProgramScope pScope,
      ParserTools pParserTools,
      CFA pCfa) {
    lineOffsetsByFile = ArrayListMultimap.create(pLineOffsetsByFile);
    logger = Objects.requireNonNull(pLogger);
    invariantWitnessFactory = Objects.requireNonNull(pInvariantWitnessFactory);
    parser = Objects.requireNonNull(pParser);
    scope = Objects.requireNonNull(pScope);
    parserTools = Objects.requireNonNull(pParserTools);
    cfa = Objects.requireNonNull(pCfa);
  }

  static InvariantStoreEntryParser getNewInvariantStoreEntryParser(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA,
      ListMultimap<String, Integer> pLineOffsetsByFile)
      throws InvalidConfigurationException {

    InvariantWitnessFactory invariantWitnessFactory =
        InvariantWitnessFactory.getFactory(pLogger, pCFA);

    // initialize the parser to convert the string to Expressions (e.g. AExpressionTree).
    CProgramScope scope = new CProgramScope(pCFA, pLogger);
    ParserTools parserTools =
        ParserTools.create(ExpressionTrees.newFactory(), pCFA.getMachineModel(), pLogger);
    CParser parser =
        CParser.Factory.getParser(
            pLogger,
            CParser.Factory.getOptions(pConfig),
            pCFA.getMachineModel(),
            pShutdownNotifier);

    return new InvariantStoreEntryParser(
        pLineOffsetsByFile, pLogger, invariantWitnessFactory, parser, scope, parserTools, pCFA);
  }

  /**
   * Parses a raw invariant-store entry to an invariant witness.
   *
   * <p>Parsing includes mapping from file-locations to CFANodes and assigning variable in the
   * invariant to decalarations.
   *
   * <p>Parsing invariants is imprecise. Thus, there are no guarantees for the quality of the
   * returned invariants. Some location or declaration mappings might be wrong. Also, some
   * invariants might appear twice.
   *
   * @param entry entry to parse
   * @return collection of invariants that might be represented by the given entry
   * @throws InterruptedException if this thread was interrputed during parsing
   */
  Collection<InvariantWitness> parseStoreEntry(LoopInvariantEntry entry)
      throws InterruptedException {
    final LocationRecord location = entry.getLocation();

    // Currently we only do very minimal validation of the witnesses we read.
    // If the witness was produced for another file we can just ignore it.
    if (!lineOffsetsByFile.containsKey(location.getFileName())) {
      logger.log(
          Level.INFO, "Invariant", entry.getLoopInvariant(), "does not apply to any input file");
      return ImmutableSet.of();
    }

    FileLocation fileLocation = parseFileLocation(location);

    // Nodes where the invariant possibly holds
    Collection<CFANode> candidateNodes =
        InvariantStoreUtil.getNodesAtFileLocation(fileLocation, cfa);
    CProgramScope functionScope = scope.withFunctionScope(location.getFunction());

    ImmutableSet.Builder<InvariantWitness> result = ImmutableSet.builder();
    for (CFANode candidateNode : candidateNodes) {
      CProgramScope scopeWithPredicate =
          functionScope.withLocationDescriptor(getNodeScopeDescriptor(candidateNode));

      ExpressionTree<AExpression> invariantFormula =
          CParserUtils.parseStatementsAsExpressionTree(
              ImmutableSet.of(entry.getLoopInvariant().getString()),
              Optional.empty(),
              parser,
              scopeWithPredicate,
              parserTools);

      if (invariantFormula.equals(ExpressionTrees.getTrue())) {
        // These are useless!
        continue;
      }

      result.add(
          invariantWitnessFactory.fromLocationAndInvariant(
              fileLocation, candidateNode, ExpressionTrees.cast(invariantFormula)));
    }

    return result.build();
  }

  /**
   * Returns a predicate that filters FileLocations that have (roughly) the same scope as the given
   * node. This is required for the parser to determine the declaration of a variable: The parser
   * tries to find a declaration of which the fileLocation satisfies the predicate (c.f. {@link
   * CProgramScope#lookupVariable(String)}).
   *
   * <p>Note that the predicate is imprecise (it might allow FileLocations outside of the scope or
   * miss some). More invariants might be usable by improving this method.
   *
   * @param node Node to find the scope for
   * @return Predicate that matches (roughly) the fileLocations with the same program-scope as the
   *     node
   */
  private static Predicate<FileLocation> getNodeScopeDescriptor(CFANode node) {
    Collection<FileLocation> possiblyUsageLocations = tryFindUsageLocations(node);

    int minOffset =
        possiblyUsageLocations.stream().map(f -> f.getNodeOffset()).min(Integer::compare).orElse(0);
    int maxOffset =
        possiblyUsageLocations.stream().map(f -> f.getNodeOffset()).max(Integer::compare).orElse(0);
    return f -> minOffset <= f.getNodeOffset() && f.getNodeOffset() <= maxOffset;
  }

  private static Collection<FileLocation> tryFindUsageLocations(CFANode rootNode) {
    ImmutableSet.Builder<FileLocation> result = ImmutableSet.builder();

    Queue<CFANode> waitlist = new ArrayDeque<>();
    Set<CFANode> visited = new HashSet<>();
    waitlist.add(rootNode);

    // This is a BFS: Start with the rootNode and get successor-nodes.
    // The heuristic is that we get all the nodes where variables have the same scope as in the root
    // node.
    // We don't go past function boundaries, ignore dummy-edges and don't follow backward edges
    // (e.g. in loops).
    // The latter stops us from exiting loop bodies or (in some cases) accidantally jump to nodes
    // with a declaration that is actually shadowed by another one at the rootNode
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.remove();
      if (visited.contains(current)) {
        continue;
      }

      visited.add(current);

      for (int leavingEdgeId = 0; leavingEdgeId < current.getNumLeavingEdges(); leavingEdgeId++) {
        CFAEdge leavingEdge = current.getLeavingEdge(leavingEdgeId);

        if (leavingEdge instanceof FunctionReturnEdge || leavingEdge instanceof FunctionCallEdge) {
          continue;
        }

        if (!leavingEdge.getFileLocation().equals(FileLocation.DUMMY)) {
          result.add(leavingEdge.getFileLocation());
        }

        if (leavingEdge.getSuccessor().getReversePostorderId() < current.getReversePostorderId()) {
          // Not a backward edge:
          waitlist.add(leavingEdge.getSuccessor());
        }
      }
    }

    return result.build();
  }

  private FileLocation parseFileLocation(LocationRecord entryLocation) {
    int offetInFile =
        lineOffsetsByFile.get(entryLocation.getFileName()).get(entryLocation.getLine() - 1);

    return new FileLocation(
        Path.of(entryLocation.getFileName()),
        offetInFile + entryLocation.getColumn(),
        0,
        entryLocation.getLine(),
        entryLocation.getLine());
  }
}
