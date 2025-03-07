// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SingleLocationFormulaInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.Invariant;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.InvariantExchangeFormatTransformer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix = "bmc.kinduction.regression.witness")
public class RegressionVerificationWitnessToCandidateInvariantsConverter {

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;
  private final CFA cfa;

  public RegressionVerificationWitnessToCandidateInvariantsConverter(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final FormulaManagerView pfmgrV,
      final PathFormulaManager pPFMgr,
      final CFA pCFA)
      throws InvalidConfigurationException {
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    formulaManagerView = pfmgrV;
    pathFormulaManager = pPFMgr;
    cfa = pCFA;
    config.inject(this);
  }

  public enum INVARIANT_MAPPING_HEURISTIC {
    ALL_LOOPS_IN_FUNCTION,
    NEAREST_LOOP_IN_FUNCTION_WITHOUT_CANDIDATE,
    LOOPS_IN_FUNCTION_IN_ORDER,
    NEAREST_N_LOOPS
  }

  @Option(
      secure = true,
      description =
          "Heuristic for generating candidate invariants from witness invariants,"
              + "which determines how witness invariants are mapped to loops ",
      name = "mappingHeuristic")
  private INVARIANT_MAPPING_HEURISTIC mappingHeuristic =
      INVARIANT_MAPPING_HEURISTIC.ALL_LOOPS_IN_FUNCTION;

  @Option(
      secure = true,
      description =
          "Enable to use NEAREST_N_LOOPS heuristic for witness invariants for "
              + "which the primary heuristic did not generate candidate invariants",
      name = "enableSecondaryMappingHeuristic")
  private boolean useLineNumbersForInvariantsNotMatchedByFunctionHeuristics = false;

  @Option(
      secure = true,
      description =
          "Maximum number of invariants candidates that are generated for a witness invariants when"
              + " ignoring the function information of the witness and only using the line number."
              + " The option has no effect if the heuristic is not NEAREST_N_LOOPS and option"
              + " enableSecondaryHeuristic is disabled.",
      name = "allowedLineMappings")
  private int numberOfCandidatesPerInvariant = 3;

  private int extractLineNumberOfNode(final CFANode pNode) {
    Preconditions.checkNotNull(pNode);
    Preconditions.checkNotNull(pNode.getFunction());
    return pNode.getFunction().getFileLocation().getStartingLineNumber();
  }

  private String extractFunctionName(final CFANode pNode) {
    Preconditions.checkNotNull(pNode);
    Preconditions.checkNotNull(pNode.getFunction());
    return pNode.getFunction().getOrigName();
  }

  private Set<Invariant> readInvariantEntriesFromWitness(final Path pWitnessFile) {
    try (InputStream witness = MoreFiles.asByteSource(pWitnessFile).openStream(); ) {
      List<AbstractEntry> entries = AutomatonWitnessV2ParserUtils.parseYAML(witness);
      return new InvariantExchangeFormatTransformer(config, logger, shutdownNotifier, cfa)
          .generateInvariantsFromEntries(entries);
    } catch (IOException | InvalidConfigurationException | InterruptedException e) {
      logger.logUserException(Level.INFO, e, "Could not parse witness file");
      return ImmutableSet.of();
    }
  }

  private ImmutableMultimap<String, CFANode> mapLoopHeadsToFunctions() {
    return FluentIterable.from(cfa.getAllLoopHeads().orElseThrow())
        .index(node -> extractFunctionName(node));
  }

  private ImmutableSet<CandidateInvariant> mapInvariantsToAllLoopLocationsWithSameFunctionName(
      final Set<Invariant> pWitnessInvariants, final Set<Invariant> pUnusedWitnessInvariants) {
    Preconditions.checkState(cfa.getAllLoopHeads().isPresent());

    ImmutableSet.Builder<CandidateInvariant> candidateInvariants = ImmutableSet.builder();
    ImmutableMultimap<String, CFANode> loopHeadsPerFunction = mapLoopHeadsToFunctions();

    BooleanFormula invFormula;
    for (Invariant invariant : pWitnessInvariants) {

      invFormula = toFormula(invariant.getFormula());
      if (invFormula == null) {
        logger.log(
            Level.INFO, "Failed to convert invariant to formula, skipping invariant " + invariant);
        continue;
      }

      if (loopHeadsPerFunction.get(invariant.getFunction()).isEmpty()) {
        pUnusedWitnessInvariants.add(invariant);
      } else {
        for (CFANode node : loopHeadsPerFunction.get(invariant.getFunction())) {
          candidateInvariants.add(
              SingleLocationFormulaInvariant.makeLocationInvariant(
                  node, invFormula, formulaManagerView));
        }
      }
    }
    return candidateInvariants.build();
  }

  private ImmutableSet<CandidateInvariant>
      mapInvariantsToClosestLoopLocationsWithoutInvariantInSameFunctionName(
          final Set<Invariant> pWitnessInvariants, final Set<Invariant> pUnusedWitnessInvariants) {
    Preconditions.checkState(cfa.getAllLoopHeads().isPresent());

    ImmutableSet.Builder<CandidateInvariant> candidates = ImmutableSet.builder();
    ImmutableMultimap<String, CFANode> loopHeadsPerFunction = mapLoopHeadsToFunctions();
    Set<CFANode> loopHeadsWithCandidateInvariants =
        Sets.newHashSetWithExpectedSize(pWitnessInvariants.size());

    BooleanFormula invFormula;
    for (Invariant invariant : pWitnessInvariants) {
      invFormula = toFormula(invariant.getFormula());
      if (invFormula == null) {
        logger.log(
            Level.INFO, "Failed to convert invariant to formula, skipping invariant " + invariant);
        continue;
      }

      CFANode loopHeadOfCandidateInvariant = null;
      Integer minimumDistance = Integer.MAX_VALUE;

      for (CFANode loopHead : loopHeadsPerFunction.get(invariant.getFunction())) {
        if (!loopHeadsWithCandidateInvariants.contains(loopHead)) {
          int distance = Math.abs(extractLineNumberOfNode(loopHead) - invariant.getLine());
          if (distance < minimumDistance) {
            minimumDistance = distance;
            loopHeadOfCandidateInvariant = loopHead;
          }
        }
      }

      if (loopHeadOfCandidateInvariant != null) {
        loopHeadsWithCandidateInvariants.add(loopHeadOfCandidateInvariant);
        candidates.add(
            SingleLocationFormulaInvariant.makeLocationInvariant(
                loopHeadOfCandidateInvariant, invFormula, formulaManagerView));
      } else {
        pUnusedWitnessInvariants.add(invariant);
      }
    }

    return candidates.build();
  }

  private ImmutableSet<CandidateInvariant> mapInvariantsInOrderToLoopLocationsWithSameFunctionName(
      final Set<Invariant> pWitnessInvariants, final Set<Invariant> pUnusedWitnessInvariants) {
    Preconditions.checkState(cfa.getAllLoopHeads().isPresent());

    ImmutableSet.Builder<CandidateInvariant> candidates = ImmutableSet.builder();
    List<CFANode> loopHeadsSortedByFunctionAndLine =
        FluentIterable.from(cfa.getAllLoopHeads().orElseThrow())
            .toSortedList(
                Comparator.comparing(this::extractFunctionName)
                    .thenComparing(Comparator.comparingInt(this::extractLineNumberOfNode)));
    List<Invariant> invariantsSortedByFunctionAndLine =
        FluentIterable.from(pWitnessInvariants)
            .toSortedList(
                Comparator.comparing(Invariant::getFunction)
                    .thenComparing(Comparator.comparingInt(Invariant::getLine)));

    BooleanFormula invFormula;
    int nextLoopHead = 0;
    String invFunName;
    for (Invariant invariant : invariantsSortedByFunctionAndLine) {
      invFunName = invariant.getFunction();
      Preconditions.checkNotNull(invFunName);

      while (loopHeadsSortedByFunctionAndLine.size() > nextLoopHead
          && invFunName.compareTo(
                  extractFunctionName(loopHeadsSortedByFunctionAndLine.get(nextLoopHead)))
              > 0) {
        nextLoopHead++;
      }

      if (loopHeadsSortedByFunctionAndLine.size() <= nextLoopHead
          || invFunName.compareTo(
                  extractFunctionName(loopHeadsSortedByFunctionAndLine.get(nextLoopHead)))
              < 0) {
        pUnusedWitnessInvariants.add(invariant);
      } else {
        invFormula = toFormula(invariant.getFormula());
        if (invFormula == null) {
          logger.log(
              Level.INFO,
              "Failed to convert invariant to formula, skipping invariant " + invariant);
          continue;
        }
        candidates.add(
            SingleLocationFormulaInvariant.makeLocationInvariant(
                loopHeadsSortedByFunctionAndLine.get(nextLoopHead++),
                invFormula,
                formulaManagerView));
      }
    }
    return candidates.build();
  }

  private ImmutableSet<CandidateInvariant> mapInvariantsToNNearestLoopLocations(
      final Set<Invariant> pWitnessInvariants) {
    Preconditions.checkState(cfa.getAllLoopHeads().isPresent());
    if (numberOfCandidatesPerInvariant <= 0) {
      logger.log(
          Level.WARNING,
          "Number of allowed candidate invariants per loop head below one. "
              + "No candidate invariants will be generated.");
      return ImmutableSet.of();
    }

    if (pWitnessInvariants.isEmpty()) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<CandidateInvariant> candidates = ImmutableSet.builder();

    NavigableMap<Integer, CFANode> loopHeadsOrderedByLine = new TreeMap<>();
    for (CFANode loopHead : cfa.getAllLoopHeads().orElseThrow()) {
      loopHeadsOrderedByLine.put(extractLineNumberOfNode(loopHead), loopHead);
    }

    BooleanFormula invFormula;
    int line;
    Integer nextHigher;
    Integer nextLower;
    Integer nextCandidateLine;
    for (Invariant invariant : pWitnessInvariants) {
      invFormula = toFormula(invariant.getFormula());
      if (invFormula == null) {
        logger.log(
            Level.INFO, "Failed to convert invariant to formula, skipping invariant " + invariant);
        continue;
      }

      line = invariant.getLine();
      nextHigher = loopHeadsOrderedByLine.ceilingKey(line);
      nextLower = loopHeadsOrderedByLine.floorKey(line);
      for (int i = 0; i < numberOfCandidatesPerInvariant; i++) {
        if (nextHigher == null && nextLower == null) {
          break;
        } else if (nextHigher == null || line - nextLower <= nextHigher - line) {
          nextCandidateLine = nextLower;
          nextLower = loopHeadsOrderedByLine.floorKey(nextCandidateLine - 1);
        } else {
          nextCandidateLine = nextHigher;
          nextHigher = loopHeadsOrderedByLine.ceilingKey(nextCandidateLine + 1);
        }
        candidates.add(
            SingleLocationFormulaInvariant.makeLocationInvariant(
                loopHeadsOrderedByLine.get(nextCandidateLine), invFormula, formulaManagerView));
      }
    }

    return candidates.build();
  }

  public ImmutableSet<CandidateInvariant> getCandidateInvariantsFromWitness(
      final Path pWitnessFile) {
    if (!cfa.getAllLoopHeads().isPresent()) {
      logger.log(
          Level.INFO,
          "Loop heads could not be identified. " + "No candidate invariants will be generated.");
      return ImmutableSet.of();
    }

    Set<Invariant> witnessInvariants = readInvariantEntriesFromWitness(pWitnessFile);
    Set<Invariant> unusedWitnessInvariants = new HashSet<>();

    ImmutableSet<CandidateInvariant> candidates =
        switch (mappingHeuristic) {
          case ALL_LOOPS_IN_FUNCTION ->
              mapInvariantsToAllLoopLocationsWithSameFunctionName(
                  witnessInvariants, unusedWitnessInvariants);
          case NEAREST_LOOP_IN_FUNCTION_WITHOUT_CANDIDATE ->
              mapInvariantsToClosestLoopLocationsWithoutInvariantInSameFunctionName(
                  witnessInvariants, unusedWitnessInvariants);
          case LOOPS_IN_FUNCTION_IN_ORDER ->
              mapInvariantsInOrderToLoopLocationsWithSameFunctionName(
                  witnessInvariants, unusedWitnessInvariants);
          case NEAREST_N_LOOPS -> mapInvariantsToNNearestLoopLocations(witnessInvariants);
        };

    if (mappingHeuristic != INVARIANT_MAPPING_HEURISTIC.NEAREST_N_LOOPS
        && useLineNumbersForInvariantsNotMatchedByFunctionHeuristics) {
      candidates =
          ImmutableSet.<CandidateInvariant>builder()
              .addAll(candidates)
              .addAll(mapInvariantsToNNearestLoopLocations(unusedWitnessInvariants))
              .build();
    }

    return candidates;
  }

  private @Nullable BooleanFormula toFormula(ExpressionTree<AExpression> pWitnessInvariant) {
    try {
      return pWitnessInvariant.accept(
          new ToFormulaVisitor(formulaManagerView, pathFormulaManager, null));
    } catch (ToFormulaException e) {
      logger.logUserException(
          Level.INFO, e, "Conversion of invariant " + pWitnessInvariant + " failed");
      return null;
    }
  }
}
// public ImmutableSet<CandidateInvariant> witnessParser(Path pfilename, Solver pSolver)
// throws JsonParseException, JsonMappingException, IOException {
// FormulaManagerView formulaManager = pSolver.getFormulaManager();
// File yamlWitness = pfilename.toFile();
// ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
// JavaType entryType =
// mapper.getTypeFactory().constructCollectionType(List.class, InvariantSetEntry.class);
// List<InvariantSetEntry> entries = mapper.readValue(yamlWitness, entryType);
// ImmutableSet.Builder<CandidateInvariant> invariants = ImmutableSet.builder();
//
// NavigableMap<Integer, CFANode> loopHeadsOrderedByLine = new TreeMap<Integer, CFANode>();
// for (CFANode loopHead : cfa.getAllLoopHeads().orElseThrow()) {
// loopHeadsOrderedByLine.put(loopHead.getEnteringEdge(0).getLineNumber(), loopHead);
// }
//
// for (InvariantSetEntry entry : entries) {
// while (!entry.content.isEmpty()) {
// InvariantEntry invariant = (InvariantEntry) entry.content.remove(0);
// Integer line = invariant.getLocation().getLine();
//
// // find nearest loophead if exact match not found
// if (!loopHeadsOrderedByLine.containsKey(line)) {
// NavigableSet<Integer> lineSet = loopHeadsOrderedByLine.navigableKeySet();
// Integer lowerLine = lineSet.lower(line);
// Integer higherLine = lineSet.higher(line);
// if (lowerLine == null && higherLine != null) {
// if (line - lowerLine < higherLine - line) {
// line = lowerLine;
// } else {
// line = higherLine;
// }
// } else {
// if (lowerLine == null) {
// line = higherLine;
// } else {
// line = lowerLine;
// }
// }
// }
// logger.log(Level.INFO, loopHeadsOrderedByLine.get(line), "Line", line);

        // BooleanFormula booleanInvariant = formulaManager.parse(invariant.getValue());
        //
        // invariants.add(
        // SingleLocationFormulaInvariant
        // .makeLocationInvariant(
        // loopHeadsOrderedByLine.get(line),
        // invariant.getValue()));
// }
// }
//
// return invariants.build();
// }
// }
