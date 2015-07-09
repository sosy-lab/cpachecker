/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci.translators;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.truth.Truth;

public class TranslatorTest {

  private String[] varNames = {"var1", "var2", "var3", "fun::var1", "fun::varB", "fun::varC"};
  private CSimpleType integervariable = new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false);
  private SSAMap ssaTest;

  @Before
  public void init() {
    SSAMapBuilder ssaBuilder = SSAMap.emptySSAMap().builder();
    ssaBuilder.setIndex("var1", integervariable, 1);
    ssaBuilder.setIndex("var3", integervariable, 1);
    ssaBuilder.setIndex("fun::varB", integervariable, 1);
    ssaTest = ssaBuilder.build();
  }

  @Test
  public void testValueTranslator() throws InvalidConfigurationException {
    PersistentMap<MemoryLocation, Value> constantsMap = PathCopyingPersistentTreeMap.of();
    PersistentMap<MemoryLocation, Type> locToTypeMap = PathCopyingPersistentTreeMap.of();

    constantsMap = constantsMap.putAndCopy(MemoryLocation.valueOf("var1"), new NumericValue(3));
    constantsMap = constantsMap.putAndCopy(MemoryLocation.valueOf("var3"), NullValue.getInstance());
    constantsMap = constantsMap.putAndCopy(MemoryLocation.valueOf("fun::var1"), new NumericValue(1.5));
    constantsMap = constantsMap.putAndCopy(MemoryLocation.valueOf("fun::varC"), new NumericValue(-5));

    Truth.assertThat(constantsMap).hasSize(4);

    ValueAnalysisState vStateTest = new ValueAnalysisState(constantsMap, locToTypeMap);
    Truth.assertThat(vStateTest.getConstantsMapView()).isNotEmpty();
    ValueRequirementsTranslator vReqTransTest = new ValueRequirementsTranslator(TestDataTools.configurationForTest().build(), ShutdownNotifier.create(), TestLogManager.getInstance());

    // Test of method getVarsInRequirements()
    List<String> varsInRequirements = vReqTransTest.getVarsInRequirements(vStateTest);
    Truth.assertThat(varsInRequirements).containsExactly("var1", "var3", "fun::var1", "fun::varC");

    // Test of method getListOfIndependentRequirements()
    List<String> listOfIndependentRequirements = vReqTransTest.getListOfIndependentRequirements(vStateTest, ssaTest);
    Truth.assertThat(listOfIndependentRequirements).containsExactly("(= var1@1 3)", "(= fun::varC -5)");
  }

  @Test
  public void testSignTranslator() throws InvalidConfigurationException {
    SignState sStateTest = SignState.TOP;
    sStateTest = sStateTest.assignSignToVariable("var1", SIGN.PLUS);
    sStateTest = sStateTest.assignSignToVariable("var2", SIGN.MINUS);
    sStateTest = sStateTest.assignSignToVariable("var3", SIGN.ZERO);
    sStateTest = sStateTest.assignSignToVariable("fun::var1", SIGN.PLUSMINUS);
    sStateTest = sStateTest.assignSignToVariable("fun::varB", SIGN.PLUS0);
    sStateTest = sStateTest.assignSignToVariable("fun::varC", SIGN.MINUS0);
    SignRequirementsTranslator sReqTransTest = new SignRequirementsTranslator(TestDataTools.configurationForTest().build(), ShutdownNotifier.create(), TestLogManager.getInstance());

    // Test method getVarsInRequirements()
    List<String> varsInReq = sReqTransTest.getVarsInRequirements(sStateTest);
    Truth.assertThat(varsInReq).containsExactlyElementsIn(Arrays.asList(varNames));

    // Test method getListOfIndependentRequirements()
    List<String> listOfIndepententReq = sReqTransTest.getListOfIndependentRequirements(sStateTest, ssaTest);
    List<String> content = new ArrayList<>();
    content.add("(> var1@1 0)");
    content.add("(< var2 0)");
    content.add("(= var3@1 0)");
    content.add("(or (> fun::var1 0) (< fun::var1 0))");
    content.add("(>= fun::varB@1 0)");
    content.add("(<= fun::varC 0)");
    Truth.assertThat(listOfIndepententReq).containsExactlyElementsIn(content);
  }

  @Test
  public void testIntervalAndCartesianTranslator() throws InvalidConfigurationException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    PersistentMap<String, Interval> intervals = PathCopyingPersistentTreeMap.of();
    PersistentMap<String, Integer> referenceMap = PathCopyingPersistentTreeMap.of();

    intervals = intervals.putAndCopy("var1", new Interval(Long.MIN_VALUE, (long) 5));
    intervals = intervals.putAndCopy("var2", new Interval((long) -7, Long.MAX_VALUE));
    intervals = intervals.putAndCopy("var3", new Interval(Long.MIN_VALUE, (long) -2));
    intervals = intervals.putAndCopy("fun::var1", new Interval((long) 0, (long) 10));
    intervals = intervals.putAndCopy("fun::varB", new Interval((long) 8, Long.MAX_VALUE));
    intervals = intervals.putAndCopy("fun::varC", new Interval((long) -15, (long) -3));

    IntervalAnalysisState iStateTest = new IntervalAnalysisState(intervals, referenceMap);
    IntervalRequirementsTranslator iReqTransTest = new IntervalRequirementsTranslator(TestDataTools.configurationForTest().build(), ShutdownNotifier.create(), TestLogManager.getInstance());

    // Test method getVarsInRequirements()
    List<String> varsInRequirements = iReqTransTest.getVarsInRequirements(iStateTest);
    Truth.assertThat(varsInRequirements).containsExactlyElementsIn(Arrays.asList(varNames));

    // Test method getListOfIndepentendRequirements()
    List<String> listOfIndependentRequirements = iReqTransTest.getListOfIndependentRequirements(iStateTest, ssaTest);
    List<String> content = new ArrayList<>();
    content.add("(<= var1@1 5)");
    content.add("(>= var2 -7)");
    content.add("(<= var3@1 -2)");
    content.add("(and (>= fun::var1 0) (<= fun::var1 10))");
    content.add("(>= fun::varB@1 8)");
    content.add("(and (>= fun::varC -15) (<= fun::varC -3))");
    Truth.assertThat(listOfIndependentRequirements).containsExactlyElementsIn(content);

    // Test method writeVarDefinition()
    Method writeVarDefinition = CartesianRequirementsTranslator.class.getDeclaredMethod("writeVarDefinition", new Class[]{List.class, SSAMap.class});
    writeVarDefinition.setAccessible(true);
    List<String> varDefinition = (List<String>) writeVarDefinition.invoke(iReqTransTest, Arrays.asList(varNames), ssaTest);
    content = new ArrayList<>();
    content.add("(declare-fun var1@1() Int)");
    content.add("(declare-fun var2() Int)");
    content.add("(declare-fun var3@1() Int)");
    content.add("(declare-fun fun::var1() Int)");
    content.add("(declare-fun fun::varB@1() Int)");
    content.add("(declare-fun fun::varC() Int)");
    Truth.assertThat(varDefinition).containsExactlyElementsIn(content);

    // Test method convertToFormula()
    Pair<List<String>, String> convertedToFormula = iReqTransTest.convertToFormula(iStateTest, ssaTest);
    Truth.assertThat(convertedToFormula.getFirst()).containsExactlyElementsIn(content);
    String s = "(define-fun req () Bool (and (and (>= fun::var1 0) (<= fun::var1 10))(and (>= fun::varB@1 8)(and (and (>= fun::varC -15) (<= fun::varC -3))(and (<= var1@1 5)(and (>= var2 -7)(<= var3@1 -2)))))))";
    Truth.assertThat(convertedToFormula.getSecond()).isEqualTo(s);

    // Test method convertToFormula() with empty IntervalAnalysisState
    convertedToFormula = iReqTransTest.convertToFormula(new IntervalAnalysisState(), ssaTest);
    Truth.assertThat(convertedToFormula.getFirst()).isEmpty();
    s = "(define-fun req () Bool true)";
    Truth.assertThat(convertedToFormula.getSecond()).isEqualTo(s);

    // Test method convertToFormula() with another IntervalAnalysisState
    intervals = PathCopyingPersistentTreeMap.of();
    referenceMap = PathCopyingPersistentTreeMap.of();
    intervals = intervals.putAndCopy("var1", new Interval((long) 0, Long.MAX_VALUE));
    IntervalAnalysisState anotherIStateTest = new IntervalAnalysisState(intervals, referenceMap);

    convertedToFormula = iReqTransTest.convertToFormula(anotherIStateTest, ssaTest);
    content = new ArrayList<>();
    content.add("(declare-fun var1@1() Int)");
    Truth.assertThat(convertedToFormula.getFirst()).containsExactlyElementsIn(content);
    s = "(define-fun req () Bool (>= var1@1 0))";
    Truth.assertThat(convertedToFormula.getSecond()).isEqualTo(s);
  }
}
