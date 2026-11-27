// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapWriter;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.regions.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.GlobalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocalLoopPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.MetadataRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionScope;

/**
 * Tests that a predicate precision is exported to a precision witness in C by the names of the
 * input program, and read back to predicates over the same variables.
 */
public class PredicatePrecisionWitnessTest extends SolverViewBasedTest0 {

  @Rule public final TemporaryFolder folder = new TemporaryFolder();

  private CFA cfa;
  private CFANode inBlock;
  private AbstractionManager abstractionManager;

  @Override
  protected ConfigurationBuilder createTestConfigBuilder() throws InvalidConfigurationException {
    // The witness to read is given by an option of a path
    return super.createTestConfigBuilder()
        .addConverter(
            FileOption.class,
            FileTypeConverter.create(
                Configuration.builder().setOption("output.disable", "true").build()));
  }

  @Before
  public void setUp() throws Exception {
    cfa = TestCfaUtils.makeCfaFromFile("test/programs/precision-exchange/scopes.c");
    inBlock = TestCfaUtils.getEdge("p.y = i__2;", cfa).getPredecessor();
    abstractionManager =
        new AbstractionManager(new SymbolicRegionManager(solver), config, logger, solver);
  }

  private AbstractionPredicate equals(String pVariable, int pValue) {
    return abstractionManager.makePredicate(
        imgrv.equal(imgrv.makeVariable(pVariable), imgrv.makeNumber(pValue)));
  }

  private Path export(
      ImmutableSetMultimap<CFANode, AbstractionPredicate> pLocal,
      ImmutableSetMultimap<String, AbstractionPredicate> pFunction,
      ImmutableSet<AbstractionPredicate> pGlobal)
      throws Exception {
    Configuration exportConfig =
        Configuration.builder()
            .copyFrom(config)
            .setOption("cpa.predicate.predmap.witnessPredicateFormats", "C")
            .build();
    Path file = folder.getRoot().toPath().resolve("precision-C.yml");
    new PredicateMapWriter(exportConfig, mgrv, logger, Optional.of(cfa))
        .writePredicateMapAsWitness(
            pLocal,
            pFunction,
            pGlobal,
            PathTemplate.ofFormatString(folder.getRoot() + "/precision-%s.yml"),
            new MetadataRecord("2.2", "uuid", "2026-01-01T00:00:00Z", null, null));
    return file;
  }

  private static String describe(PrecisionScope pScope) {
    return switch (pScope) {
      case GlobalPrecisionScope _ -> "global";
      case FunctionPrecisionScope function -> "function " + function.getFunctionName();
      case LocalPrecisionScope local ->
          "location "
              + local.getLocation().getLine()
              + ":"
              + local.getLocation().getColumn().orElseThrow();
      case LocalLoopPrecisionScope loop -> "loop " + loop.getLocation().getLine();
    };
  }

  private static ImmutableSetMultimap<String, String> valuesPerScope(Path pWitness)
      throws Exception {
    ImmutableSetMultimap.Builder<String, String> values = ImmutableSetMultimap.builder();
    for (PrecisionExchangeEntry entry :
        ((PrecisionExchangeSetEntry) AutomatonWitnessV2ParserUtils.parseYAML(pWitness).getFirst())
            .getContent()) {
      values.putAll(describe(entry.scope()), entry.values());
    }
    return values.build();
  }

  private PredicatePrecision readBack(Path pWitness) throws Exception {
    Configuration importConfig =
        Configuration.builder()
            .copyFrom(config)
            .setOption("cpa.predicate.abstraction.initialPredicates", pWitness.toString())
            .build();
    return new PredicatePrecisionBootstrapper(
            importConfig,
            logger,
            cfa,
            abstractionManager,
            mgrv,
            ShutdownNotifier.createDummy(),
            new PathFormulaManagerImpl(
                mgrv,
                importConfig,
                logger,
                ShutdownNotifier.createDummy(),
                cfa,
                AnalysisDirection.FORWARD),
            null)
        .prepareInitialPredicates();
  }

  private ImmutableSet<String> variablesOf(Iterable<AbstractionPredicate> pPredicates) {
    return FluentIterable.from(pPredicates)
        .transformAndConcat(
            predicate -> mgrv.extractVariableNames(mgrv.uninstantiate(predicate.getSymbolicAtom())))
        .toSet();
  }

  @Test
  public void locationScopeNamesTheVariablesInScope() throws Exception {
    Path witness =
        export(
            ImmutableSetMultimap.<CFANode, AbstractionPredicate>builder()
                .putAll(
                    inBlock,
                    equals("main::i", 0),
                    equals("main::i__2", 7),
                    equals("main::shadowed__1", 1),
                    equals("g", 3),
                    equals("static__counter__calls", 1))
                .build(),
            ImmutableSetMultimap.of(),
            ImmutableSet.of());

    // The outer i is hidden in the block, and calls is only in scope in its function
    assertThat(valuesPerScope(witness))
        .isEqualTo(
            ImmutableSetMultimap.of(
                "location 34:5", "( i == 7 )",
                "location 34:5", "( shadowed == 1 )",
                "location 34:5", "( g == 3 )"));
    assertThat(variablesOf(readBack(witness).getLocalPredicates().get(inBlock)))
        .containsExactly("main::i__2", "main::shadowed__1", "g");
  }

  @Test
  public void functionAndGlobalScopesNameOnlyVariablesOfTheInputProgram() throws Exception {
    Path witness =
        export(
            ImmutableSetMultimap.of(),
            ImmutableSetMultimap.<String, AbstractionPredicate>builder()
                .putAll("main", equals("main::i", 0), equals("main::shadowed__1", 1))
                .putAll(
                    "counter",
                    equals("static__counter__calls", 1),
                    equals("counter::__retval__", 1))
                .build(),
            ImmutableSet.of(equals("g", 3), equals("static__counter__calls", 1)));

    // main has three variables i, and the global scope cannot name a static local variable
    assertThat(valuesPerScope(witness))
        .isEqualTo(
            ImmutableSetMultimap.of(
                "global", "( g == 3 )",
                "function main", "( shadowed == 1 )",
                "function counter", "( calls == 1 )"));
    PredicatePrecision precision = readBack(witness);
    assertThat(variablesOf(precision.getGlobalPredicates())).containsExactly("g");
    // A precision has its global predicates also in every function
    assertThat(variablesOf(precision.getFunctionPredicates().get("main")))
        .containsExactly("main::shadowed__1", "g");
    assertThat(variablesOf(precision.getFunctionPredicates().get("counter")))
        .containsExactly("static__counter__calls", "g");
  }
}
