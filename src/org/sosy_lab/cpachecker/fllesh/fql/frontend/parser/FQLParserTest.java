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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.parser;

import java.io.StringReader;

import org.junit.Test;


public class FQLParserTest {

  // TODO Use Michael's test suite, too.

  @Test
  public void testFQLParserScanner001() throws Exception {
    String lInput = "COVER STATES(@BASICBLOCKENTRY)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner002() throws Exception {
    String lInput = "COVER STATES(@CONDITIONEDGE)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner003() throws Exception {
    String lInput = "COVER STATES(@DECISIONEDGE)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner004() throws Exception {
    String lInput = "COVER STATES(@CONDITIONGRAPH)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner005() throws Exception {
    String lInput = "COVER STATES(ID)";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner006() throws Exception {
    String lInput = "COVER STATES(COMPLEMENT(@BASICBLOCKENTRY))";

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
    String lInput = "IN @FILE(\"foo.c\") COVER EDGES(UNION(@BASICBLOCKENTRY, @CONDITIONEDGE))";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner010() throws Exception {
    String lInput = "IN @FILE(\"foo.c\") COVER EDGES(UNION(@BASICBLOCKENTRY, @CONDITIONEDGE)) PASSING ID*.@5.ID*";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner011() throws Exception {
    String lInput = "IN @FILE(\"foo.c\") COVER STATES(@CONDITIONEDGE, { x > 10}) PASSING ID*.@5.ID*";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner012() throws Exception {
    String lInput = "IN @FILE(\"foo.c\") COVER { y <= 53049 } STATES(@CONDITIONEDGE, { x > 10}) PASSING ID*.@5.ID*";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

  @Test
  public void testFQLParserScanner013() throws Exception {
    String lInput = "IN @FILE(\"foo.c\") COVER { y <= 53049 } STATES(@CONDITIONEDGE, { x > 10}) PASSING ID*.@5.ID*";

    System.out.println(lInput);

    FQLParser lParser = new FQLParser(lInput);

    System.out.println("RESULT: " + lParser.parse().value.toString());
  }

}
