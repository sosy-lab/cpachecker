/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator;


public class StatisiticInfo {

  /*
   * TestCase
   */

  public String testcase;

  /*
   * Configuration
   */

  public String policy;
  public String mapping;

  /*
   * CFA
   */
  public int locs;
  public int edges;

  /*
   * Generation
   */
  public double gen_time;
  public int gen_states;
  public int merge_time;
  public int reachset;
  public float proof_time;

  /*
   * Certificate
   */
  public int cert_size;

  /*
   * Checking
   */
  public double check_time;
  public int check_states;

  @Override
  public String toString() {
    StringBuffer bf=new StringBuffer();
    bf.append(testcase);
    bf.append("\t");
    bf.append(policy);
    bf.append("\t");
    bf.append(mapping);
    bf.append("\t");
    bf.append(locs);
    bf.append("\t");
    bf.append(edges);
    bf.append("\t");
    bf.append(gen_time);
    bf.append("\t");
    bf.append(gen_states);
    bf.append("\t");
    bf.append(merge_time);
    bf.append("\t");
    bf.append(reachset);
    bf.append("\t");
    bf.append(proof_time);
    bf.append("\t");
    bf.append(cert_size);
    bf.append("\t");
    bf.append(check_time);
    bf.append("\t");
    bf.append(check_states);
    bf.append("\t");
    return bf.toString();
  }

}
