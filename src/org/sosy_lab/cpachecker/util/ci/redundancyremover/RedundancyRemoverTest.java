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
package org.sosy_lab.cpachecker.util.ci.redundancyremover;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.sign.SignState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.truth.Truth;


public class RedundancyRemoverTest {

  private final MachineModel machineModel = MachineModel.LINUX32;

  @Test
  public void testCompareIntervalState() {
    // use following pairs, test that correct expected value
    // test for each pair (a,b) that compare(a,b)=-compare(b,a)
    // both unbound intervals (expected 0)
    // intervals [-5,8], [0,5] (expected 1)
    // intervals [-1,3], [-1,7] (expected -1)
    // intervals [3,9], [5,7] (expected -1)

    RedundantRequirementsRemoverIntervalStateImplementation intervalStateImpl = new RedundantRequirementsRemoverIntervalStateImplementation();

    Interval i01 = Interval.createUnboundInterval();
    Interval i02 = Interval.createUnboundInterval();
    Truth.assertThat(intervalStateImpl.compare(i02, i01)).isEqualTo(0);
    Truth.assertThat(intervalStateImpl.compare(i01, i02)).isEqualTo(0);

    Interval i11 = new Interval(-5L, 8L);
    Interval i12 = new Interval(0L, 5L);
    Truth.assertThat(intervalStateImpl.compare(i11, i12)).isEqualTo(1);

    Interval i21 = new Interval(-1L, 3L);
    Interval i22 = new Interval(-1L, 7L);
    Truth.assertThat(intervalStateImpl.compare(i21, i22)).isEqualTo(-1);

    Interval i31 = new Interval(3L, 9L);
    Interval i32 = new Interval(5L, 7L);
    Truth.assertThat(intervalStateImpl.compare(i31, i32)).isEqualTo(1); // TODO eigentlich -1
  }

  @Test
  public void testCoversIntervalState() {
    // [-3,5], [0,1] (true)
    // [1,1],[1,1] (two distinct objects) (true)
    // unbound interval, [3,7] (true)
    // [-3,5], [-3,8] (false)
    // [1,7],[-5,7] (false)
    // [0,3],[9,12] (false)

    RedundantRequirementsRemoverIntervalStateImplementation intervalStateImpl = new RedundantRequirementsRemoverIntervalStateImplementation();

    Interval i_35 = new Interval(-3L, 5L);
    Interval i01 = new Interval(0L, 1L);
    Truth.assertThat(intervalStateImpl.covers(i_35, i01)).isEqualTo(true);

    Interval i11a = new Interval(1L, 1L);
    Interval i11b = new Interval(1L, 1L);
    Truth.assertThat(intervalStateImpl.covers(i11a, i11b)).isEqualTo(true);

    Interval iUnbounded = Interval.createUnboundInterval();
    Interval i37 = new Interval(3L, 7L);
    Truth.assertThat(intervalStateImpl.covers(iUnbounded, i37)).isEqualTo(true);

    Interval i_38 = new Interval(-3L, 8L);
    Truth.assertThat(intervalStateImpl.covers(i_35, i_38)).isEqualTo(false);

    Interval i17 = new Interval(1L, 7L);
    Interval i_57 = new Interval(-5L, 7L);
    Truth.assertThat(intervalStateImpl.covers(i17, i_57)).isEqualTo(false);

    Interval i03 = new Interval(0L, 3L);
    Interval i912 = new Interval(9L, 12L);
    Truth.assertThat(intervalStateImpl.covers(i03, i912)).isEqualTo(false);
  }

  @Test
  public void testGetAbstractValueIntervalState() {
    // IntervalAnalysisState intervalState = new IntervalAnalysisState().addInterval("x", new Interval(-1L,4L), 0);
    // varOrConst 1 -> Interval [1,1]
    // varOrConst x -> Interval [-1,4]
    // varOrConst y -> unbound interval

    IntervalAnalysisState intervalState1 = new IntervalAnalysisState().addInterval("1", new Interval(1L, 1L), 0);
    //IntervalAnalysisState intervalState2 = new IntervalAnalysisState().addInterval("x", new Interval(-1L, 4L), 0);
    IntervalAnalysisState intervalState3 = new IntervalAnalysisState().addInterval("y", Interval.createUnboundInterval(), 0);

    RedundantRequirementsRemoverIntervalStateImplementation intervalStateImpl = new RedundantRequirementsRemoverIntervalStateImplementation();
    Truth.assertThat(intervalStateImpl.getAbstractValue(intervalState1, "1")).isEqualTo(new Interval(1L, 1L));
//    Truth.assertThat(intervalStateImpl.getAbstractValue(intervalState2, "x")).isEqualTo(new Interval(-1L, 4L)); // TODO
    Truth.assertThat(intervalStateImpl.getAbstractValue(intervalState3, "y")).isEqualTo(Interval.createUnboundInterval());
  }

  @Test
  public void testCompareSignState() {
    // use following pairs, test that correct expected value
    // test for each pair (a,b) that compare(a,b)=-compare(b,a)
    RedundantRequirementsRemoverSignStateImplementation signImpl = new RedundantRequirementsRemoverSignStateImplementation();
    // EMPTY, EMPTY (expected 0)

    Truth.assertThat(signImpl.compare(SIGN.EMPTY, SIGN.EMPTY)).isEqualTo(0);
    // EMPTY, PLUS (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUS, SIGN.EMPTY)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.EMPTY, SIGN.PLUS)).isEqualTo(-1);
    // EMPTY, MINUS (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.MINUS, SIGN.EMPTY)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.EMPTY, SIGN.MINUS)).isEqualTo(-1);
    // EMPTY, ZERO (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.ZERO, SIGN.EMPTY)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.EMPTY, SIGN.ZERO)).isEqualTo(-1);
    // EMPTY, PLUSMINUS (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUSMINUS, SIGN.EMPTY)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.EMPTY, SIGN.PLUSMINUS)).isEqualTo(-1);
    // EMPTY, PLUS0 (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUS0, SIGN.EMPTY)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.EMPTY, SIGN.PLUS0)).isEqualTo(-1);
    // EMPTY, MINUS0 (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.MINUS0, SIGN.EMPTY)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.EMPTY, SIGN.MINUS0)).isEqualTo(-1);
    // EMPTY, ALL (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.ALL, SIGN.EMPTY)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.EMPTY, SIGN.ALL)).isEqualTo(-1);
    // PLUS, PLUS (expected 0)
    Truth.assertThat(signImpl.compare(SIGN.PLUS, SIGN.PLUS)).isEqualTo(0);
    // PLUS, MINUS (expected 1)
    Truth.assertThat(signImpl.compare(SIGN.MINUS, SIGN.PLUS)).isEqualTo(-1);
    Truth.assertThat(signImpl.compare(SIGN.PLUS, SIGN.MINUS)).isEqualTo(1);
    // PLUS, ZERO (expected 1)
    Truth.assertThat(signImpl.compare(SIGN.ZERO, SIGN.PLUS)).isEqualTo(-1);
    Truth.assertThat(signImpl.compare(SIGN.PLUS, SIGN.ZERO)).isEqualTo(1);
    // PLUS, PLUSMINUS (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUSMINUS, SIGN.PLUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.PLUS, SIGN.PLUSMINUS)).isEqualTo(-1);
    // PLUS, PLUS0 (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUS0, SIGN.PLUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.PLUS, SIGN.PLUS0)).isEqualTo(-1);
    // PLUS, MINUS0 (expected 1)
    Truth.assertThat(signImpl.compare(SIGN.MINUS0, SIGN.PLUS)).isEqualTo(-1);
    Truth.assertThat(signImpl.compare(SIGN.PLUS, SIGN.MINUS0)).isEqualTo(1);
    // PLUS, ALL (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.ALL, SIGN.PLUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.PLUS, SIGN.ALL)).isEqualTo(-1);
    // MINUS, MINUS (expected 0)
    Truth.assertThat(signImpl.compare(SIGN.MINUS, SIGN.MINUS)).isEqualTo(0);
    // MINUS, ZERO (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.ZERO, SIGN.MINUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.MINUS, SIGN.ZERO)).isEqualTo(-1);
    // MINUS, PLUSMINUS (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUSMINUS, SIGN.MINUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.MINUS, SIGN.PLUSMINUS)).isEqualTo(-1);
    // MINUS, PLUS0 (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUS0, SIGN.MINUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.MINUS, SIGN.PLUS0)).isEqualTo(-1);
    // MINUS, MINUS0 (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.MINUS0, SIGN.MINUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.MINUS, SIGN.MINUS0)).isEqualTo(-1);
    // MINUS, ALL (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.ALL, SIGN.MINUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.MINUS, SIGN.ALL)).isEqualTo(-1);
    // ZERO, ZERO (expected 0)
    Truth.assertThat(signImpl.compare(SIGN.ZERO, SIGN.ZERO)).isEqualTo(0);
    // ZERO, PLUSMINUS (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUSMINUS, SIGN.ZERO)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.ZERO, SIGN.PLUSMINUS)).isEqualTo(-1);
    // ZERO, PLUS0 (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUS0, SIGN.ZERO)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.ZERO, SIGN.PLUS0)).isEqualTo(-1);
    // ZERO, MINUS0 (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.MINUS0, SIGN.ZERO)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.ZERO, SIGN.MINUS0)).isEqualTo(-1);
    // ZERO, ALL (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.ALL, SIGN.ZERO)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.ZERO, SIGN.ALL)).isEqualTo(-1);
    // PLUSMINUS, PLUSMINUS (expected 0)
    Truth.assertThat(signImpl.compare(SIGN.PLUSMINUS, SIGN.PLUSMINUS)).isEqualTo(0);
    // PLUSMINUS, PLUS0 (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.PLUS0, SIGN.PLUSMINUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.PLUSMINUS, SIGN.PLUS0)).isEqualTo(-1);
    // PLUSMINUS, MINUS0 (expected 1)
    Truth.assertThat(signImpl.compare(SIGN.MINUS0, SIGN.PLUSMINUS)).isEqualTo(-1);
    Truth.assertThat(signImpl.compare(SIGN.PLUSMINUS, SIGN.MINUS0)).isEqualTo(1);
    // PLUSMINUS, ALL (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.ALL, SIGN.PLUSMINUS)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.PLUSMINUS, SIGN.ALL)).isEqualTo(-1);
    // PLUS0, PLUS0 (expected 0)
    Truth.assertThat(signImpl.compare(SIGN.PLUS0, SIGN.PLUS0)).isEqualTo(0);
    // PLUS0, MINUS0 (expected 1)
    Truth.assertThat(signImpl.compare(SIGN.MINUS0, SIGN.PLUS0)).isEqualTo(-1);
    Truth.assertThat(signImpl.compare(SIGN.PLUS0, SIGN.MINUS0)).isEqualTo(1);
    // PLUS0, ALL (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.ALL, SIGN.PLUS0)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.PLUS0, SIGN.ALL)).isEqualTo(-1);
    // MINUS0, MINUS0 (expected 0)
    Truth.assertThat(signImpl.compare(SIGN.MINUS0, SIGN.MINUS0)).isEqualTo(0);
    // MINUS0, ALL (expected -1)
    Truth.assertThat(signImpl.compare(SIGN.ALL, SIGN.MINUS0)).isEqualTo(1);
    Truth.assertThat(signImpl.compare(SIGN.MINUS0, SIGN.ALL)).isEqualTo(-1);
    // ALL, ALL (expected 0)
    Truth.assertThat(signImpl.compare(SIGN.ALL, SIGN.ALL)).isEqualTo(0);
  }

  @Test
  public void testCoversSignState() {
    // PLUS, MINUS (false)
    // PLUS0, ZERO (true)
    // MINUS0, PLUSMINUS (false)
    // ALL, EMPTY (true)
    RedundantRequirementsRemoverSignStateImplementation signImpl = new RedundantRequirementsRemoverSignStateImplementation();
    Truth.assertThat(signImpl.covers(SIGN.PLUS, SIGN.MINUS)).isFalse();
    Truth.assertThat(signImpl.covers(SIGN.PLUS0, SIGN.ZERO)).isTrue();
    Truth.assertThat(signImpl.covers(SIGN.MINUS0, SIGN.PLUSMINUS)).isFalse();
    Truth.assertThat(signImpl.covers(SIGN.ALL, SIGN.EMPTY)).isTrue();

  }

  @Test
  public void testGetAbstractValueSignState() {
    // SignState signState = SignState.TOP.assignSignToVariable("x", SIGN.PLUSMINUS);
    // varOrConst -1 -> MINUS
    // varOrConst 0 -> ZERO
    // varOrConst 1 -> PLUS
    // varOrConst x -> PLUSMINUS
    // varOrConst y -> ALL
    RedundantRequirementsRemoverSignStateImplementation signImpl = new RedundantRequirementsRemoverSignStateImplementation();
    SignState signState1 = SignState.TOP.assignSignToVariable("-1", SIGN.MINUS);
    SignState signState2 = SignState.TOP.assignSignToVariable("0", SIGN.ZERO);
    SignState signState3 = SignState.TOP.assignSignToVariable("1", SIGN.PLUS);
    SignState signState4 = SignState.TOP.assignSignToVariable("x", SIGN.PLUSMINUS);
    SignState signState5 = SignState.TOP.assignSignToVariable("y", SIGN.ALL);

    Truth.assertThat(signImpl.getAbstractValue(signState1, "-1")).isEqualTo(SIGN.MINUS);
    Truth.assertThat(signImpl.getAbstractValue(signState2, "0")).isEqualTo(SIGN.ZERO);
    Truth.assertThat(signImpl.getAbstractValue(signState3, "1")).isEqualTo(SIGN.PLUS);
    Truth.assertThat(signImpl.getAbstractValue(signState4, "x")).isEqualTo(SIGN.PLUSMINUS);
    Truth.assertThat(signImpl.getAbstractValue(signState5, "y")).isEqualTo(SIGN.ALL);
  }

  @Test
  public void testCompareValueAnalysisState() {
    // use following pairs, test that correct expected value
    // test for each pair (a,b) that compare(a,b)=-compare(b,a)
    // both unknown value (expected 0)
    // unknown value and numeric value -3 (expected 1)
    // both Numeric value 7 (expected 0)
    // Numeric value -5 and numeric value 9 (expected -1)

    RedundantRequirementsValueAnalysisStateImplementation valueImpl = new RedundantRequirementsValueAnalysisStateImplementation();

    Value v11 = Value.UnknownValue.getInstance();
    Value v12 = Value.UnknownValue.getInstance();
    Truth.assertThat(valueImpl.compare(v11, v12)).isEqualTo(0);

    v11 = Value.UnknownValue.getInstance();
    v12 = new NumericValue(-3);
    Truth.assertThat(valueImpl.compare(v12, v11)).isEqualTo(-1);
    Truth.assertThat(valueImpl.compare(v11, v12)).isEqualTo(1);

    v11 = new NumericValue(7);
    v12 = new NumericValue(7);
    Truth.assertThat(valueImpl.compare(v11, v12)).isEqualTo(0);

    v11 = new NumericValue(-5);
    v12 = new NumericValue(9);
    Truth.assertThat(valueImpl.compare(v12, v11)).isEqualTo(14);
    Truth.assertThat(valueImpl.compare(v11, v12)).isEqualTo(-14); // TODO eigentlich -1
  }

  @Test
  public void testCoversValueAnalysisState() {
    // unknown value, Numeric Value 7 (true)
    // Numeric Value 7, Numeric Value 7 (two distinct objects) (true)
    // Numeric Value 7, Numeric Value -4 (false)

    RedundantRequirementsValueAnalysisStateImplementation valueImpl = new RedundantRequirementsValueAnalysisStateImplementation();

    Value v11 = Value.UnknownValue.getInstance();
    Value v12 = new NumericValue(7);
    Truth.assertThat(valueImpl.covers(v11, v12)).isTrue();

    v11 = new NumericValue(7);
    v12 = new NumericValue(7);
    Truth.assertThat(valueImpl.covers(v11, v12)).isTrue();

    v11 = new NumericValue(7);
    v12 = new NumericValue(-4);
    Truth.assertThat(valueImpl.covers(v11, v12)).isFalse();
  }

  @Test
  public void testGetAbstractValueValueAnalysisState() {
    // ValueAnalysisState valState = new ValueAnalysisState();
    // valState.assignConstant(MemoryLocation.valueOf("x"), new NumericValue(7), new CSimpleType(
    //    false, false, CBasicType.INT, false, false, false, false, false, false, false));
    // varOrConst 1 -> NumericValue with value 1
    // varOrConst x -> NumericValue with value 7
    // varOrConst y -> UnknownValue

    RedundantRequirementsValueAnalysisStateImplementation valueImpl = new RedundantRequirementsValueAnalysisStateImplementation();

    ValueAnalysisState valState1 = new ValueAnalysisState(machineModel);
    NumericValue val1 = new NumericValue(1L);
    valState1.assignConstant(MemoryLocation.valueOf("1"), val1, new CSimpleType(
        false, false, CBasicType.INT, false, false, false, false, false, false, false));
    //    Truth.assertThat(valueImpl.getAbstractValue(valState1, "1")).isEqualTo(val1); // TODO

    ValueAnalysisState valState2 = new ValueAnalysisState(machineModel);
    NumericValue val2 = new NumericValue(7L);
    valState2.assignConstant(MemoryLocation.valueOf("x"), val2, new CSimpleType(
        false, false, CBasicType.INT, false, false, false, false, false, false, false));
    Truth.assertThat(valueImpl.getAbstractValue(valState2, "x")).isEqualTo(val2);

    ValueAnalysisState valState3 = new ValueAnalysisState(machineModel);
    Value val3 = Value.UnknownValue.getInstance();
    valState3.assignConstant(MemoryLocation.valueOf("y"), val3, new CSimpleType(
        false, false, CBasicType.INT, false, false, false, false, false, false, false));
    Truth.assertThat(valueImpl.getAbstractValue(valState3, "y")).isEqualTo(val3);
  }

  @Test
  public void testIdentifyAndRemoveRedundantRequirements() {//throws IOException, ParserException, InterruptedException, InvalidConfigurationException {
//    CFA cfa = TestDataTools.makeCFA("void main() { int x = 5;}");
//    AbstractState loc =
//        new LocationCPA(cfa, TestDataTools.configurationForTest().build()).getInitialState(
//            cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
//
//    final int NUMCONSTRAINTS = 5;
//    List<String> input = new ArrayList<>(2);
//    input.add("x");
//    input.add("y");
//    List<String> output = new ArrayList<>(2);
//    output.add("u");
//    output.add("v");
//    Pair<List<String>, List<String>> inout = Pair.of(input, output);
//    List<Pair<List<String>, List<String>>> inputOutputSignatures = new ArrayList<>(NUMCONSTRAINTS);
//    for(int i=0;i<NUMCONSTRAINTS;i++) {
//      inputOutputSignatures.add(inout);
//    }
//
//    List<Pair<ARGState, Collection<ARGState>>> requirements, expectedResult, result;
//    ValueAnalysisState valState;
//    ARGState argState;
//    Collection<ARGState> ends;
//
//    requirements = new ArrayList<>(NUMCONSTRAINTS);
//    expectedResult = new ArrayList<>();


//    requirements.add(Pair.of(new ARGState(new CompositeState()), second));





    //result = RedundantRequirementsRemover.removeRedundantRequirements(requirements, inputOutputSignatures, ValueAnalysisState.class);
//     test Truth.assertThat(result).containsExactlyElementsIn(expectedResult);

  }

}
