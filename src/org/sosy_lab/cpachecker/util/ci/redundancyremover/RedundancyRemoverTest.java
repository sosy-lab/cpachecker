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

import java.io.IOException;

import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.exceptions.ParserException;


public class RedundancyRemoverTest {

  @Test
  public void testCompareIntervalState() {
    // TODO
    // use following pairs, test that correct expected value
    // test for each pair (a,b) that compare(a,b)=-compare(b,a)
    // both unbound intervals (expected 0)
    // intervals [-5,8], [0,5] (expected 1)
    // intervals [-1,3], [-1,7] (expected -1)
    // intervals [3,9], [5,7] (expected -1)
  }

  @Test
  public void testCoversIntervalState() {
    // TODO
    // [-3,5], [0,1] (true)
    // [1,1],[1,1] (two distinct objects) (true)
    // unbound interval, [3,7] (true)
    // [-3,5], [-3,8] (false)
    // [1,7],[-5,7] (false)
    // [0,3],[9,12] (false)
  }

  @Test
  public void testGetAbstractValueIntervalState() {
    // IntervalAnalysisState intervalState = new IntervalAnalysisState().addInterval("x", new Interval(-1L,4L), 0);
    // TODO
    // varOrConst 1 -> Interval [1,1]
    // varOrConst x -> Interval [-1,4]
    // varOrConst y -> unbound interval
  }

  @Test
  public void testCompareSignState() {
    // TODO
    // use following pairs, test that correct expected value
    // test for each pair (a,b) that compare(a,b)=-compare(b,a)
    // EMPTY, EMPTY (expected 0)
    // EMPTY, PLUS (expected -1)
    // EMPTY, MINUS (expected -1)
    // EMPTY, ZERO (expected -1)
    // EMPTY, PLUSMINUS (expected -1)
    // EMPTY, PLUS0 (expected -1)
    // EMPTY, MINUS0 (expected -1)
    // EMPTY, ALL (expected -1)
    // PLUS, PLUS (expected 0)
    // PLUS, MINUS (expected 1)
    // PLUS, ZERO (expected 1)
    // PLUS, PLUSMINUS (expected -1)
    // PLUS, PLUS0 (expected -1)
    // PLUS, MINUS0 (expected 1)
    // PLUS, ALL (expected -1)
    // MINUS, MINUS (expected 0)
    // MINUS, ZERO (expected -1)
    // MINUS, PLUSMINUS (expected -1)
    // MINUS, PLUS0 (expected -1)
    // MINUS, MINUS0 (expected -1)
    // MINUS, ALL (expected -1)
    // ZERO, ZERO (expected 0)
    // ZERO, PLUSMINUS (expected -1)
    // ZERO, PLUS0 (expected -1)
    // ZERO, MINUS0 (expected -1)
    // ZERO, ALL (expected -1)
    // PLUSMINUS, PLUSMINUS (expected 0)
    // PLUSMINUS, PLUS0 (expected -1)
    // PLUSMINUS, MINUS0 (expected 1)
    // PLUSMINUS, ALL (expected -1)
    // PLUS0, PLUS0 (expected 0)
    // PLUS0, MINUS0 (expected 1)
    // PLUS0, ALL (expected -1)
    // MINUS0, MINUS0 (expected 0)
    // MINUS0, ALL (expected -1)
    // ALL, ALL (expected 0)
  }

  @Test
  public void testCoversSignState() {
    // TODO
    // PLUS, MINUS (false)
    // PLUS0, ZERO (true)
    // MINUS0, PLUSMINUS (false)
    // ALL, EMPTY (true)
  }

  @Test
  public void testGetAbstractValueSignState() {
    // SignState signState = SignState.TOP.assignSignToVariable("x", SIGN.PLUSMINUS);
    // TODO
    // varOrConst -1 -> MINUS
    // varOrConst 0 -> ZERO
    // varOrConst 1 -> PLUS
    // varOrConst x -> PLUSMINUS
    // varOrConst y -> ALL
  }

  @Test
  public void testCompareValueAnalysisState() {
    // TODO
    // use following pairs, test that correct expected value
    // test for each pair (a,b) that compare(a,b)=-compare(b,a)
    // both unknown value (expected 0)
    // unknown value and numeric value -3 (expected 1)
    // both Numeric value 7 (expected 0)
    // Numeric value -5 and numeric value 9 (expected -1)
  }

  @Test
  public void testCoversValueAnalysisState() {
    // TODO
    // unknown value, Numeric Value 7 (true)
    // Numeric Value 7, Numeric Value 7 (two distinct objects) (true)
    // Numeric Value 7, Numeric Value -4 (false)
  }

  @Test
  public void testGetAbstractValueValueAnalysisState() {
    // ValueAnalysisState valState = new ValueAnalysisState();
    // valState.assignConstant(MemoryLocation.valueOf("x"), new NumericValue(7), new CSimpleType(
    //    false, false, CBasicType.INT, false, false, false, false, false, false, false));
    // TODO
    // varOrConst 1 -> NumericValue with value 1
    // varOrConst x -> NumericValue with value 7
    // varOrConst y -> UnknownValue
  }

  @Test
  public void testIdentifyAndRemoveRedundantRequirements() throws IOException, ParserException, InterruptedException, InvalidConfigurationException {
    /*CFA cfa = TestDataTools.makeCFA("void main() { int x = 5;}");
    AbstractState loc =
        new LocationCPA(cfa, TestDataTools.configurationForTest().build()).getInitialState(
            cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

    final int NUMCONSTRAINTS = 5;
    List<String> input = new ArrayList<>(2);
    input.add("x");
    input.add("y");
    List<String> output = new ArrayList<>(2);
    output.add("u");
    output.add("v");
    Pair<List<String>, List<String>> inout = Pair.of(input, output);
    List<Pair<List<String>, List<String>>> inputOutputSignatures = new ArrayList<>(NUMCONSTRAINTS);
    for(int i=0;i<NUMCONSTRAINTS;i++) {
      inputOutputSignatures.add(inout);
    }

    List<Pair<ARGState, Collection<ARGState>>> requirements, expectedResult, result;
    ValueAnalysisState valState;
    ARGState argState;
    Collection<ARGState> ends;

    requirements = new ArrayList<>(NUMCONSTRAINTS);
    expectedResult = new ArrayList<>();


    requirements.add(Pair.of(new ARGState(new CompositeState()), second))





    result = RedundantRequirementsRemover.removeRedundantRequirements(requirements, inputOutputSignatures, ValueAnalysisState.class);
*/    // test Truth.assertThat(result).containsExactlyElementsIn(expectedResult);

  }

}
