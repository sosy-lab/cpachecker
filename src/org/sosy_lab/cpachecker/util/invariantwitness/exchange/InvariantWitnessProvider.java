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
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.AbstractInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitnessFactory;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantStoreEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantStoreEntryLocation;

@Options(prefix = "invariantStore.import")
public class InvariantWitnessProvider {
  @Option(
      secure = true,
      required = true,
      description = "The directory where the invariants are stored.")
  private Path storeDirectory;

  private final Table<String, Integer, Integer> lineOffsetsByFile;
  private final LogManager logger;
  private final InvariantWitnessFactory invariantWitnessFactory;
  private final CParser parser;
  private final CProgramScope scope;
  private final ParserTools parserTools;
  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  private InvariantWitnessProvider(
      Configuration pConfig, CFA pCFA, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    lineOffsetsByFile = InvariantStoreUtil.getLineOffsetsByFile(pCFA.getFileNames());
    invariantWitnessFactory = InvariantWitnessFactory.getFactory(pLogger, pCFA);

    scope = new CProgramScope(pCFA, pLogger);
    parserTools = ParserTools.create(ExpressionTrees.newFactory(), pCFA.getMachineModel(), pLogger);
    parser =
        CParser.Factory.getParser(
            pLogger,
            CParser.Factory.getOptions(pConfig),
            pCFA.getMachineModel(),
            pShutdownNotifier);
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
        throw new UnsupportedOperationException("Can only produce expression trees");
      }

      @Override
      public ExpressionTreeSupplier getExpressionTreeSupplier()
          throws CPAException, InterruptedException {
        // TODO Thread-safety: Is the parser thread-safe?
        ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
        Collection<InvariantWitness> witnesses = provider.getCurrentWitnesses();
        Map<CFANode, ExpressionTree<Object>> witnessesByNode =
            witnesses.stream()
                .collect(
                    Collectors.toMap(
                        InvariantWitness::getNode, InvariantWitness::getFormula, factory::or));

        return new ExpressionTreeSupplier() {

          @Override
          public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
            return witnessesByNode.getOrDefault(pNode, ExpressionTrees.getTrue());
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
    ImmutableCollection.Builder<InvariantWitness> resultBuilder = ImmutableSet.builder();
    try (DirectoryStream<Path> stream =
        Files.newDirectoryStream(storeDirectory, (p) -> p.toFile().isFile())) {
      for (Path file : stream) {
        // TODO Filter out processed files
        resultBuilder.addAll(parseStoreFile(file));
      }
    } catch (IOException | DirectoryIteratorException x) {
      // TODO
    }

    return resultBuilder.build();
  }

  private Collection<InvariantWitness> parseStoreFile(Path file)
      throws IOException, InterruptedException {
    InvariantStoreEntry entry = mapper.readValue(file.toFile(), InvariantStoreEntry.class);

    if (!lineOffsetsByFile.containsRow(entry.getLocation().getFileName())) {
      logger.log(
          Level.INFO, "Invariant " + file.toFile().getName() + " does not apply to any input file");
      return Collections.emptySet();
    }

    FileLocation fileLocation = parseFileLocation(entry.getLocation());
    ExpressionTree<Object> invariantFormula =
        parseAssertString(entry.getLoopInvariant().getString(), entry.getLocation().function);
    return invariantWitnessFactory.fromFileLocationAndInvariant(fileLocation, invariantFormula);
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

  private ExpressionTree<Object> parseAssertString(String assertString, String functionName)
      throws InterruptedException {
    return ExpressionTrees.cast(
        CParserUtils.parseStatementsAsExpressionTree(
            Set.of(assertString),
            Optional.empty(),
            parser,
            scope.withFunctionScope(functionName),
            parserTools));
  }
}
