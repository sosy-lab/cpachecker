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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
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

@Options(prefix = "invariantStore.import")
public class InvariantWitnessProvider {
  @Option(
      secure = true,
      required = true,
      description = "The directory where the invariants are stored.")
  private String storeDirectory;

  private final Table<String, Integer, Integer> lineOffsetsByFile;
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
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    cfa = pCFA;
    lineOffsetsByFile = InvariantStoreUtil.getLineOffsetsByFile(pCFA.getFileNames());
    invariantWitnessFactory = InvariantWitnessFactory.getFactory(pLogger, pCFA);

    knownWitnesses = new HashSet<>();

    scope = new CProgramScope(pCFA, pLogger);
    parserTools = ParserTools.create(ExpressionTrees.newFactory(), pCFA.getMachineModel(), pLogger);
    parser =
        CParser.Factory.getParser(
            pLogger,
            CParser.Factory.getOptions(pConfig),
            pCFA.getMachineModel(),
            pShutdownNotifier);

    try {
      // Watch service watches directory for new added files.
      watchService = FileSystems.getDefault().newWatchService();
      Paths.get(storeDirectory)
          .register(
              watchService,
              StandardWatchEventKinds.ENTRY_MODIFY,
              StandardWatchEventKinds.ENTRY_CREATE);
    } catch (IOException e) {
      throw new InvalidConfigurationException("Can not use store directory: " + storeDirectory);
    }
  }

  public static InvariantWitnessProvider getProvider(
      Configuration pConfig, CFA pCFA, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new InvariantWitnessProvider(pConfig, pCFA, pLogger, pShutdownNotifier);
  }

  public static InvariantGenerator getInvariantGenerator(
      Configuration pConfig, CFA pCFA, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
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

  public Collection<InvariantWitness> getCurrentWitnesses() throws InterruptedException {
    Set<InvariantWitness> newWitnesses = parseNewWitnessFiles();
    knownWitnesses.addAll(newWitnesses);

    return Collections.unmodifiableSet(knownWitnesses);
  }

  private Set<InvariantWitness> parseNewWitnessFiles() throws InterruptedException {
    WatchKey key = watchService.poll();
    if (key == null) {
      // No new files were added.
      return Set.of();
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

  public Map<CFANode, ExpressionTree<Object>> getCurrentWitnessesByNodes()
      throws InterruptedException {
    // TODO Thread-safety: Is the parser thread-safe?
    ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
    Collection<InvariantWitness> witnesses = getCurrentWitnesses();
    return witnesses.stream()
        .collect(
            Collectors.toMap(InvariantWitness::getNode, InvariantWitness::getFormula, factory::or));
  }

  private Collection<InvariantWitness> parseStoreFile(File file)
      throws IOException, InterruptedException {
    InvariantStoreEntry entry = mapper.readValue(file, InvariantStoreEntry.class);

    if (!lineOffsetsByFile.containsRow(entry.getLocation().getFileName())) {
      logger.log(Level.INFO, "Invariant " + file.getName() + " does not apply to any input file");
      return Collections.emptySet();
    }

    FileLocation fileLocation = parseFileLocation(entry.getLocation());

    String rawInvariant = entry.getLoopInvariant().getString();
    String functionName = entry.getLocation().getFunction();

    Collection<CFANode> candidateNodes = InvariantStoreUtil.getNodeCandiadates(fileLocation, cfa);

    CProgramScope functionScope = scope.withFunctionScope(functionName);
    ImmutableSet.Builder<InvariantWitness> result = ImmutableSet.builder();
    for (CFANode candidateNode : candidateNodes) {
      Collection<FileLocation> possiblyUsageLocations = tryFindUsageLocations(candidateNode);

      if (possiblyUsageLocations.isEmpty()) {
        continue;
      }

      int minOffset =
          possiblyUsageLocations.stream()
              .map(f -> f.getNodeOffset())
              .min(Integer::compare)
              .orElse(0);
      int maxOffset =
          possiblyUsageLocations.stream()
              .map(f -> f.getNodeOffset())
              .max(Integer::compare)
              .orElse(0);

      ExpressionTree<Object> invariantFormula =
          ExpressionTrees.cast(
              CParserUtils.parseStatementsAsExpressionTree(
                  Set.of(rawInvariant),
                  Optional.empty(),
                  parser,
                  functionScope.withLocationDescriptor(
                      f -> minOffset <= f.getNodeOffset() && f.getNodeOffset() <= maxOffset),
                  parserTools));

      if (invariantFormula.equals(ExpressionTrees.getTrue())) {
        // These are useless!
        continue;
      }

      result.add(
          invariantWitnessFactory.fromLocationAndInvariant(
              fileLocation, candidateNode, invariantFormula));
    }

    return result.build();
  }

  private Collection<FileLocation> tryFindUsageLocations(CFANode rootNode) {
    ImmutableSet.Builder<FileLocation> result = ImmutableSet.builder();

    Queue<CFANode> waitlist = new ArrayDeque<>();
    Set<CFANode> visited = new HashSet<>();
    waitlist.add(rootNode);

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
          // Not a backward edge
          waitlist.add(leavingEdge.getSuccessor());
        }
      }
    }

    return result.build();
  }

  private FileLocation parseFileLocation(InvariantStoreEntryLocation entryLocation) {
    int offetInFile = lineOffsetsByFile.get(entryLocation.getFileName(), entryLocation.getLine());

    return new FileLocation(
        entryLocation.getFileName(),
        offetInFile + entryLocation.getColumn(),
        0,
        entryLocation.getLine(),
        entryLocation.getLine());
  }
}
