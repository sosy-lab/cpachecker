// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversLines;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.SourceLocationMatcher.LineMatcher;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.ViolationSequenceEntry;

@Options(prefix = "witness")
public class AutomatonYAMLParser {

  @Option(
      secure = true,
      name = "invariantsSpecificationAutomaton",
      description =
          "Validate correctness witness by specifying an invariants specification automaton")
  private InvariantsSpecificationAutomatonBuilder invariantsSpecAutomaton =
      InvariantsSpecificationAutomatonBuilder.NO_ISA;

  @Option(secure = true, description = "File for exporting the witness automaton in DOT format.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path automatonDumpFile = null;

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final ParserTools parserTools;
  final CParser cparser;

  private Scope scope;

  public AutomatonYAMLParser(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA,
      Scope pScope)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    scope = pScope;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCFA;
    config = pConfig;
    parserTools = ParserTools.create(ExpressionTrees.newFactory(), cfa.getMachineModel(), logger);
    cparser =
        CParser.Factory.getParser(
            /*
             * FIXME: Use normal logger as soon as CParser supports parsing
             * expression trees natively, such that we can remove the workaround
             * with the undefined __CPAchecker_ACSL_return dummy function that
             * causes warnings to be logged.
             */
            LogManager.createNullLogManager(),
            CParser.Factory.getOptions(config),
            cfa.getMachineModel(),
            shutdownNotifier);
  }

  public static List<AbstractEntry> parseYAML(InputStream pInputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    List<AbstractEntry> entries =
        Arrays.asList(mapper.readValue(pInputStream, AbstractEntry[].class));
    return entries;
  }

  private static WitnessType getWitnessType(InputStream pInputStream) throws IOException {
    // Parse the YAML document
    List<AbstractEntry> entries = parseYAML(pInputStream);
    for (AbstractEntry e : entries) {
      if (e instanceof ViolationSequenceEntry) {
        return WitnessType.VIOLATION_WITNESS;
      }
    }

    return WitnessType.CORRECTNESS_WITNESS;
  }

  public static WitnessType getWitnessType(Path pPath)
      throws InvalidConfigurationException, InterruptedException {
    return AutomatonGraphmlParser.handlePotentiallyGZippedInput(
        MoreFiles.asByteSource(pPath),
        AutomatonYAMLParser::getWitnessType,
        WitnessParseException::new);
  }

  public Automaton parseAutomatonFile(Path pInputFile)
      throws InvalidConfigurationException, InterruptedException {
    return AutomatonGraphmlParser.handlePotentiallyGZippedInput(
        MoreFiles.asByteSource(pInputFile), this::parseAutomatonFile, WitnessParseException::new);
  }

  private Scope determineScope(
      Optional<String> pExplicitScope,
      Deque<String> pFunctionStack,
      Predicate<FileLocation> pLocationDescriptor,
      Scope pScope) {
    Scope result = pScope;
    if (result instanceof CProgramScope) {
      result = ((CProgramScope) result).withLocationDescriptor(pLocationDescriptor);
      if (pExplicitScope.isPresent() || !pFunctionStack.isEmpty()) {
        final String functionName;
        if (pExplicitScope.isPresent()) {
          functionName = pExplicitScope.orElseThrow();
        } else {
          functionName = pFunctionStack.peek();
        }
        result = ((CProgramScope) result).withFunctionScope(functionName);
      }
    }
    return result;
  }

  /**
   * Parses a specification from an InputStream and returns the Automata found in the file.
   *
   * @param pInputStream the input stream to parse the witness from.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws IOException if there occurs an IOException while reading from the stream.
   * @return the automata representing the witnesses found in the stream.
   */
  private Automaton parseAutomatonFile(InputStream pInputStream)
      throws InvalidConfigurationException, IOException, InterruptedException {
    List<AbstractEntry> entries = parseYAML(pInputStream);

    String automatonName = "";
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();
    String entryStateId = "singleState";

    List<AutomatonTransition> transitions = new ArrayList<>();

    for (AbstractEntry entry : entries) {
      if (entry instanceof LoopInvariantEntry) {
        LoopInvariantEntry loopInvariantEntry = (LoopInvariantEntry) entry;

        Optional<String> resultFunction =
            Optional.of(loopInvariantEntry.getLocation().getFunction());

        LineMatcher lineMatcher =
            new LineMatcher(
                Optional.empty(), // TODO: Is this correct?
                loopInvariantEntry.getLocation().getLine(),
                loopInvariantEntry.getLocation().getLine());

        Deque<String> callStack = new ArrayDeque<>();
        callStack.push(loopInvariantEntry.getLocation().getFunction());

        Scope candidateScope = determineScope(resultFunction, callStack, lineMatcher, scope);

        ExpressionTree<AExpression> invariant =
            CParserUtils.parseStatementsAsExpressionTree(
                ImmutableSet.of(loopInvariantEntry.getLoopInvariant().getString()),
                resultFunction,
                cparser,
                candidateScope,
                parserTools);

        if (invariant.equals(ExpressionTrees.getTrue())) {
          continue;
        }

        transitions.add(
            new AutomatonTransition.Builder(
                    new CheckCoversLines(
                        ImmutableSet.of(loopInvariantEntry.getLocation().getLine())),
                    entryStateId)
                .withCandidateInvariants(invariant)
                .build());
        automatonName = loopInvariantEntry.getMetadata().getUuid();
      } else {
        throw new WitnessParseException(
            "The witness contained other statements than Loop Invariants!");
      }
    }

    List<AutomatonInternalState> automatonStates =
        ImmutableList.of(new AutomatonInternalState(entryStateId, transitions, false, false, true));

    Automaton automaton;
    try {
      automaton = new Automaton(automatonName, automatonVariables, automatonStates, entryStateId);
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException(
          "The witness automaton generated from the provided YAML Witness is invalid!", e);
    }

    automaton = invariantsSpecAutomaton.build(automaton, config, logger, shutdownNotifier, cfa);

    if (automatonDumpFile != null) {
      try (Writer w = IO.openOutputFile(automatonDumpFile, Charset.defaultCharset())) {
        automaton.writeDotFile(w);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
      }
      Path automatonFile =
          automatonDumpFile.resolveSibling(automatonDumpFile.getFileName() + ".spc");
      try (Writer w = IO.openOutputFile(automatonFile, Charset.defaultCharset())) {
        w.write(automaton.toString());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
      }
    }

    return automaton;
  }
}
