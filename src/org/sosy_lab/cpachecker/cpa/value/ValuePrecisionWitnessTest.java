// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.GlobalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocalLoopPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionType;

/**
 * Tests that a precision of the value analysis is exported to a precision witness by the names of
 * the input program, and read back to the same memory locations.
 */
public class ValuePrecisionWitnessTest {

  private static final MemoryLocation OUTER_I = MemoryLocation.forLocalVariable("main", "i");
  private static final MemoryLocation INNER_I = MemoryLocation.forLocalVariable("main", "i__2");
  private static final MemoryLocation LOCAL_SHADOWED =
      MemoryLocation.forLocalVariable("main", "shadowed__1");
  private static final MemoryLocation P_Y = MemoryLocation.forLocalVariable("main", "p", 4);
  private static final MemoryLocation G = MemoryLocation.forIdentifier("g");
  private static final MemoryLocation CALLS =
      MemoryLocation.forIdentifier("static__counter__calls");
  private static final MemoryLocation RETURN_OF_COUNTER =
      MemoryLocation.forLocalVariable("counter", "__retval__");
  private static final MemoryLocation V = MemoryLocation.forLocalVariable("square", "v");
  private static final MemoryLocation LOOP_I = MemoryLocation.forLocalVariable("main", "i__1");
  private static final MemoryLocation B = MemoryLocation.forLocalVariable("choose", "b");

  private static CFA cfa;
  private static CFANode inBlock;
  private static CFANode inCounter;
  private static CFANode loopHead;
  private static CFANode callInConditionalExpression;

  @BeforeClass
  public static void createCfa() throws Exception {
    cfa = TestCfaUtils.makeCfaFromFile("test/programs/precision-exchange/scopes.c");
    inBlock = TestCfaUtils.getEdge("p.y = i__2;", cfa).getPredecessor();
    inCounter = TestCfaUtils.getEdge("return static__counter__calls;", cfa).getPredecessor();
    loopHead = Iterables.getOnlyElement(cfa.getLoopStructure().orElseThrow().getAllLoopHeads());
    callInConditionalExpression =
        CFAUtils.allEdges(cfa)
            .filter(FunctionCallEdge.class)
            .firstMatch(call -> call.getPredecessor().getFunctionName().equals("choose"))
            .get()
            .getPredecessor();
  }

  private static VariableTrackingPrecision precision(
      String pSharing, Multimap<CFANode, MemoryLocation> pTracked)
      throws InvalidConfigurationException {
    Configuration config = Configuration.builder().setOption("precision.sharing", pSharing).build();
    return VariableTrackingPrecision.createRefineablePrecision(
            config,
            VariableTrackingPrecision.createStaticPrecision(
                config, Optional.empty(), ValueAnalysisCPA.class))
        .withIncrement(pTracked);
  }

  private static String describe(PrecisionScope pScope) {
    return switch (pScope) {
      case GlobalPrecisionScope _ -> "global";
      case FunctionPrecisionScope function -> "function " + function.getFunctionName();
      case LocalPrecisionScope local -> "location " + describe(local.getLocation());
      case LocalLoopPrecisionScope loop -> "loop " + describe(loop.getLocation());
    };
  }

  private static String describe(LocationRecord pLocation) {
    return pLocation.getLine() + ":" + pLocation.getColumn().orElseThrow();
  }

  private static ImmutableSetMultimap<String, String> valuesPerScope(
      List<PrecisionExchangeEntry> pEntries) {
    ImmutableSetMultimap.Builder<String, String> values = ImmutableSetMultimap.builder();
    for (PrecisionExchangeEntry entry : pEntries) {
      values.putAll(describe(entry.scope()), entry.values());
    }
    return values.build();
  }

  private static Multimap<CFANode, MemoryLocation> readBack(
      List<PrecisionExchangeEntry> pEntries, VariableTrackingPrecision pPrecision) {
    return ValueAnalysisCPA.precisionExchangeSetToMapping(
        pEntries, cfa, pPrecision, LogManager.createTestLogManager());
  }

  @Test
  public void locationScopeNamesTheVariablesInScope() throws Exception {
    VariableTrackingPrecision precision =
        precision(
            "LOCATION",
            ImmutableSetMultimap.<CFANode, MemoryLocation>builder()
                .putAll(inBlock, OUTER_I, INNER_I, LOCAL_SHADOWED, P_Y, G, CALLS)
                .putAll(inCounter, CALLS, RETURN_OF_COUNTER)
                .build());

    List<PrecisionExchangeEntry> entries = precision.asWitnessEntries(cfa);

    // The outer i is hidden in the block, and calls is only in scope in its function
    assertThat(valuesPerScope(entries))
        .isEqualTo(
            ImmutableSetMultimap.builder()
                .putAll("location 34:5", "&i", "&shadowed", "&p.y", "&g")
                .putAll("location 22:3", "&calls")
                .build());
    Multimap<CFANode, MemoryLocation> readBack = readBack(entries, precision);
    assertThat(readBack.get(inBlock)).containsExactly(INNER_I, LOCAL_SHADOWED, P_Y, G);
    assertThat(readBack.get(inCounter)).containsExactly(CALLS);
  }

  @Test
  public void functionScopeNamesOnlyVariablesOfTheInputProgram() throws Exception {
    VariableTrackingPrecision precision =
        precision(
            "SCOPE",
            ImmutableSetMultimap.<CFANode, MemoryLocation>builder()
                .putAll(inBlock, OUTER_I, LOCAL_SHADOWED, P_Y, G, CALLS, RETURN_OF_COUNTER, V)
                .build());

    List<PrecisionExchangeEntry> entries = precision.asWitnessEntries(cfa);

    // main has three variables i, and the global scope cannot name a static local variable
    assertThat(valuesPerScope(entries))
        .isEqualTo(
            ImmutableSetMultimap.builder()
                .putAll("global", "&g")
                .putAll("function main", "&shadowed", "&p.y")
                .putAll("function square", "&v")
                .build());
  }

  @Test
  public void functionScopeIsReadBackForPrecisionOfScopes() throws Exception {
    VariableTrackingPrecision precision =
        precision(
            "SCOPE",
            ImmutableSetMultimap.<CFANode, MemoryLocation>builder()
                .putAll(inBlock, LOCAL_SHADOWED, P_Y, G, V)
                .build());

    assertThat(readBack(precision.asWitnessEntries(cfa), precision).values())
        .containsExactly(G, LOCAL_SHADOWED, P_Y, V);
  }

  @Test
  public void loopScopeIsAtTheHeadOfTheLoop() throws Exception {
    VariableTrackingPrecision precision =
        precision("LOCATION", ImmutableSetMultimap.of(loopHead, LOOP_I, loopHead, G));

    List<PrecisionExchangeEntry> entries = precision.asWitnessEntries(cfa);

    assertThat(valuesPerScope(entries))
        .isEqualTo(ImmutableSetMultimap.of("loop 29:3", "&i", "loop 29:3", "&g"));
    assertThat(readBack(entries, precision).get(loopHead)).containsExactly(LOOP_I, G);
  }

  @Test
  public void nodeInsideStatementIsInScopeOfItsFunction() throws Exception {
    VariableTrackingPrecision precision =
        precision("LOCATION", ImmutableSetMultimap.of(callInConditionalExpression, B));

    // The node is only reached in one branch of the conditional expression, so it is not the
    // location of the statement
    assertThat(valuesPerScope(precision.asWitnessEntries(cfa)))
        .isEqualTo(ImmutableSetMultimap.of("function choose", "&b"));
  }

  @Test
  public void functionEntryIsInScopeOfItsFunction() throws Exception {
    // The edges of the entry are the ones of a call, which is in another function
    VariableTrackingPrecision precision =
        precision("LOCATION", ImmutableSetMultimap.of(cfa.getFunctionHead("square"), V));

    assertThat(valuesPerScope(precision.asWitnessEntries(cfa)))
        .isEqualTo(ImmutableSetMultimap.of("function square", "&v"));
  }

  @Test
  public void locationWithoutColumnIsMatchedByItsLine() throws Exception {
    VariableTrackingPrecision precision = precision("LOCATION", ImmutableSetMultimap.of());
    List<PrecisionExchangeEntry> entries =
        ImmutableList.of(
            new PrecisionExchangeEntry(
                YAMLWitnessExpressionType.C,
                new LocalPrecisionScope(
                    new LocationRecord(
                        "test/programs/precision-exchange/scopes.c", 34, null, "main")),
                PrecisionType.MEMORY_LOCATIONS,
                ImmutableList.of("&i")));

    assertThat(readBack(entries, precision).asMap())
        .containsExactly(inBlock, ImmutableList.of(INNER_I));
  }
}
