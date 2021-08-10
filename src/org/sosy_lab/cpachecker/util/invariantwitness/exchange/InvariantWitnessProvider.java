// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.core.algorithm.invariants.AbstractInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.DownwardCastingVisitor;
import org.sosy_lab.cpachecker.util.expressions.DownwardCastingVisitor.IncompatibleLeafTypesException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessFactory;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantStoreEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantStoreEntryLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Represents an invariant witness source.
 *
 * <p>If you want to get parsed invariants (as they are used throughout CPAchecker), use {@link
 * #getInvariantGenerator}. Otherwise, if you want {@link InvariantWitness} (used as exchange object
 * in the invariant store) use {@link #getCurrentWitnesses}.
 *
 * <p>A note for future implementations: This class gets invariants from a configured directory. If
 * you want to add other strategies, consider not inheriting from this class but configuring it.
 * This could e.g. be done with a InvariantSource enum with a "getInvariants" method that is called
 * in {@link #getCurrentWitnesses}
 */
@Options(prefix = "invariantStore.import")
public final class InvariantWitnessProvider {
  @Option(secure = true, description = "The directory where the invariants are stored.")
  private String storeDirectory = "output/invariantWitnesses";

  private final Map<String, List<Integer>> lineOffsetsByFile;
  private final LogManager logger;
  private final InvariantWitnessFactory invariantWitnessFactory;
  private final CParser parser;
  private final CProgramScope scope;
  private final ParserTools parserTools;
  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
  private final CFA cfa;
  private final Set<InvariantWitness> knownWitnesses;
  private final WatchService watchService;

  private InvariantWitnessProvider(
      Configuration pConfig, CFA pCFA, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, InterruptedException {
    pConfig.inject(this);
    logger = pLogger;
    cfa = pCFA;
    lineOffsetsByFile = InvariantStoreUtil.getLineOffsetsByFile(pCFA.getFileNames());
    invariantWitnessFactory = InvariantWitnessFactory.getFactory(pLogger, pCFA);

    knownWitnesses = ConcurrentHashMap.newKeySet();

    // initialize the parser to convert the string to Expressions (e.g. AExpressionTree).
    scope = new CProgramScope(pCFA, pLogger);
    parserTools = ParserTools.create(ExpressionTrees.newFactory(), pCFA.getMachineModel(), pLogger);
    parser =
        CParser.Factory.getParser(
            pLogger,
            CParser.Factory.getOptions(pConfig),
            pCFA.getMachineModel(),
            pShutdownNotifier);

    // initialize Watch service. It's an API to watch for new files in a directory.
    // This watch service can be used to let this class block until new invariants are available.
    Path storePath = Paths.get(storeDirectory);
    try {
      watchService = FileSystems.getDefault().newWatchService();
      storePath.register(
          watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

    } catch (IOException e) {
      throw new InvalidConfigurationException("Can not use store directory: " + storeDirectory);
    }
    // Load already present files
    ImmutableSet.Builder<InvariantWitness> resultBuilder = ImmutableSet.builder();
    try (DirectoryStream<Path> stream =
        Files.newDirectoryStream(storePath, (p) -> p.toFile().isFile())) {
      for (Path file : stream) {
        resultBuilder.addAll(parseStoreFile(file.toFile()));
      }
      knownWitnesses.addAll(resultBuilder.build());
    } catch (IOException e) {
      throw new InvalidConfigurationException("Can not use store directory: " + storeDirectory);
    }

  }

  /**
   * Returns a new instance of this class. The instance is configured according to the given config.
   *
   * @param pConfig Configuration with which the instance shall be created
   * @param pCFA CFA representing the program of the invariants that the instance loads
   * @param pLogger Logger
   * @param pShutdownNotifier ShutdownNotifier
   * @return Instance of this class
   * @throws InvalidConfigurationException if the configuration is (semantically) invalid
   */
  public static InvariantWitnessProvider getProvider(
      Configuration pConfig, CFA pCFA, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, InterruptedException {
    return new InvariantWitnessProvider(pConfig, pCFA, pLogger, pShutdownNotifier);
  }

  /**
   * Returns an {@link InvariantGenerator} view of a new InvariantWitnessProdvider instance. The
   * generator returns unmodifiable suppliers. That is the suppliers only represent a snapshot and
   * are not updated automatically.
   *
   * <p>The generator supports {@link InvariantSupplier}s and {@link ExpressionTreeSupplier}s.
   *
   * <p>The generator produces invariants only on-demand (i.e. when a supplier is requested) and in
   * the calling thread. However, this behvior might change in the future.
   *
   * @param pConfig Configuration with which the instance shall be created
   * @param pCFA CFA representing the program of the invariants that the instance loads
   * @param pLogger Logger
   * @param pShutdownNotifier ShutdownNotifier
   * @return generator that gets invariants from an instance of this class
   * @throws InvalidConfigurationException if the configuration is (semantically) invalid
   */
  public static InvariantGenerator getInvariantGenerator(
      Configuration pConfig, CFA pCFA, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, InterruptedException {
    InvariantWitnessProvider provider = getProvider(pConfig, pCFA, pLogger, pShutdownNotifier);
    return new AbstractInvariantGenerator() {

      @Override
      public InvariantSupplier getSupplier() throws CPAException, InterruptedException {
        return new InvariantSupplier() {
          private final Map<CFANode, ExpressionTree<Object>> witnessesByNode =
              provider.getCurrentWitnessesByNodes();
          private final DownwardCastingVisitor<Object, AExpression> caster =
              new DownwardCastingVisitor<>(AExpression.class);

          @Override
          public BooleanFormula getInvariantFor(
              CFANode pNode,
              Optional<CallstackStateEqualsWrapper> pCallstackInformation,
              FormulaManagerView pFmgr,
              PathFormulaManager pPfmgr,
              @Nullable PathFormula pContext)
              throws InterruptedException {
            ExpressionTree<Object> invariant =
                witnessesByNode.getOrDefault(pNode, ExpressionTrees.getTrue());

            ToFormulaVisitor visitor = new ToFormulaVisitor(pFmgr, pPfmgr, pContext);

            try {
              return invariant.accept(caster).accept(visitor);
            } catch (ToFormulaException e) {
              pLogger.logDebugException(e);
            } catch (IncompatibleLeafTypesException e) {
              // This is an unexpected programming error.
              // We should never see an ExpressionTree that is not ExprTree<AExpression>
              throw new AssertionError(e);
            }

            return pFmgr.getBooleanFormulaManager().makeTrue();
          }
        };
      }

      @Override
      public ExpressionTreeSupplier getExpressionTreeSupplier()
          throws CPAException, InterruptedException {

        return new ExpressionTreeSupplier() {
          private final Map<CFANode, ExpressionTree<Object>> witnessesByNode =
              provider.getCurrentWitnessesByNodes();

          @Override
          public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
            return ExpressionTrees.cast(
                witnessesByNode.getOrDefault(pNode, ExpressionTrees.getTrue()));
          }
        };
      }

      @Override
      protected void startImpl(CFANode pInitialLocation) {
        return;
      }

      @Override
      public void cancel() {
        return;
      }

      @Override
      public boolean isProgramSafe() {
        return false;
      }
    };
  }

  /**
   * Returns a snapshot of the currently available invariant witnesses. The returned collection will
   * not update automatically and returns all the invariants that are known (not only new ones since
   * the last invocation of this method).
   *
   * <p>Note that calling this method might trigger the provider to check for and parse new
   * invariants, which is a potentially longer running operation.
   *
   * @return Current witnesses
   */
  public synchronized Collection<InvariantWitness> getCurrentWitnesses()
      throws InterruptedException {
    Set<InvariantWitness> newWitnesses = parseNewWitnessFiles();
    knownWitnesses.addAll(newWitnesses);

    return ImmutableSet.copyOf(knownWitnesses);
  }

  private Set<InvariantWitness> parseNewWitnessFiles() throws InterruptedException {
    WatchKey key = watchService.poll();
    if (key == null) {
      // No new files were added.
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<InvariantWitness> resultBuilder = ImmutableSet.builder();
    for (WatchEvent<?> event : key.pollEvents()) {
      if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
        // An "error" event that we can ignore
        continue;
      }

      // At this point we know that context is a path.
      Path newFilePath = (Path) event.context();
      File newFile = Paths.get(storeDirectory).resolve(newFilePath).toFile();

      try {
        resultBuilder.addAll(parseStoreFile(newFile));
      } catch (IOException e) {
        logger.log(Level.INFO, e, "Could not parse invariant store file");
      }
    }

    return resultBuilder.build();
  }

  private Map<CFANode, ExpressionTree<Object>> getCurrentWitnessesByNodes()
      throws InterruptedException {
    ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
    Collection<InvariantWitness> witnesses = getCurrentWitnesses();

    return witnesses.stream()
        .collect(
            // This well-named collector produces a disjunction of all invariant witness formulas
            // that hold at the same node.
            ImmutableMap.toImmutableMap(
                InvariantWitness::getNode, InvariantWitness::getFormula, factory::or));
  }

  private Collection<InvariantWitness> parseStoreFile(File file)
      throws IOException, InterruptedException {
    InvariantStoreEntry entry = mapper.readValue(file, InvariantStoreEntry.class);
    final InvariantStoreEntryLocation location = entry.getLocation();

    // Currently we only do very minimal validation of the witnesses we read.
    // If the witness was produced for another file we can just ignore it.
    if (!lineOffsetsByFile.containsKey(location.getFileName())) {
      logger.log(Level.INFO, "Invariant " + file.getName() + " does not apply to any input file");
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

  private FileLocation parseFileLocation(InvariantStoreEntryLocation entryLocation) {
    int offetInFile =
        lineOffsetsByFile.get(entryLocation.getFileName()).get(entryLocation.getLine() - 1);

    return new FileLocation(
        entryLocation.getFileName(),
        offetInFile + entryLocation.getColumn(),
        0,
        entryLocation.getLine(),
        entryLocation.getLine());
  }
}
