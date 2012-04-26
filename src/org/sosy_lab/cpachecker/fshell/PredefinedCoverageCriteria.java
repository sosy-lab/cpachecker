/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell;

public class PredefinedCoverageCriteria {

  public static final String STATEMENT_COVERAGE = "COVER \"EDGES(ID)*\".NODES(ID).\"EDGES(ID)*\"";
  public static final String STATEMENT_2_COVERAGE = STATEMENT_COVERAGE + ".NODES(ID).\"EDGES(ID)*\"";
  public static final String STATEMENT_3_COVERAGE = STATEMENT_2_COVERAGE + ".NODES(ID).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_COVERAGE = "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_2_COVERAGE = BASIC_BLOCK_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_3_COVERAGE = BASIC_BLOCK_2_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_4_COVERAGE = BASIC_BLOCK_3_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_5_COVERAGE = BASIC_BLOCK_4_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String CONDITION_COVERAGE = "COVER \"EDGES(ID)*\".EDGES(@CONDITIONEDGE).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_NODES_COVERAGE = "COVER \"EDGES(ID)*\".NODES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_NODES_2_COVERAGE = BASIC_BLOCK_NODES_COVERAGE + ".NODES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_NODES_3_COVERAGE = BASIC_BLOCK_NODES_2_COVERAGE + ".NODES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BOUNDED_PATH_1_COVERAGE = "COVER \"EDGES(ID)*\".PATHS(ID, 1).\"EDGES(ID)*\"";
  public static final String BOUNDED_PATH_2_COVERAGE = "COVER \"EDGES(ID)*\".PATHS(ID, 2).\"EDGES(ID)*\"";
  public static final String BOUNDED_PATH_3_COVERAGE = "COVER \"EDGES(ID)*\".PATHS(ID, 3).\"EDGES(ID)*\"";
  public static final String BOUNDED_PATH_4_COVERAGE = "COVER \"EDGES(ID)*\".PATHS(ID, 4).\"EDGES(ID)*\"";
  public static final String BOUNDED_PATH_5_COVERAGE = "COVER \"EDGES(ID)*\".PATHS(ID, 5).\"EDGES(ID)*\"";
  public static final String BOUNDED_PATH_6_COVERAGE = "COVER \"EDGES(ID)*\".PATHS(ID, 6).\"EDGES(ID)*\"";
  public static final String BOUNDED_PATH_7_COVERAGE = "COVER \"EDGES(ID)*\".PATHS(ID, 7).\"EDGES(ID)*\"";
  public static final String BOUNDED_PATH_8_COVERAGE = "COVER \"EDGES(ID)*\".PATHS(ID, 8).\"EDGES(ID)*\"";

}
