/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.fql2.parser;

import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;


public class FQLParserTest {

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    org.sosy_lab.cpachecker.core.CPAchecker.logger = null;
  }

  // TODO Use Michael's test suite, too.

  @Test
  public void testFQLParserScanner001() throws Exception {
    String lInput = "COVER EDGES(@BASICBLOCKENTRY)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner002() throws Exception {
    String lInput = "COVER EDGES(@CONDITIONEDGE)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner003() throws Exception {
    String lInput = "COVER EDGES(@DECISIONEDGE)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner004() throws Exception {
    String lInput = "COVER EDGES(@CONDITIONGRAPH)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner005() throws Exception {
    String lInput = "COVER EDGES(ID)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner006() throws Exception {
    String lInput = "COVER EDGES(COMPLEMENT(@BASICBLOCKENTRY))";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner007() throws Exception {
    String lInput = "COVER EDGES(INTERSECT(@BASICBLOCKENTRY, @CONDITIONEDGE))";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner008() throws Exception {
    String lInput = "COVER EDGES(UNION(@BASICBLOCKENTRY, @CONDITIONEDGE))";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner009() throws Exception {
    String lInput = "COVER \"EDGES(ID)*\"";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }
  
  @Test
  public void testFQLParserScanner010() throws Exception {
    String lInput = "COVER \"EDGES(ID)*\".EDGES(@CALL(f)).\"EDGES(ID)*\"";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

}
