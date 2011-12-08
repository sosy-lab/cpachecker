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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.HashMap;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.redlog.Rational;

public class TemplateNetwork {

  private Vector<Transition> trans;
  private TemplateMap tmap;

  public TemplateNetwork(TemplateMap tmap, Transition... trans) {
  	Vector<Transition> tvect = new Vector<Transition>();
  	for (int i = 0; i < trans.length; i++) {
  		tvect.add(trans[i]);
  	}
  	build(tmap, tvect);
  }

  public TemplateNetwork(TemplateMap tmap, Vector<Transition> trans) {
    build(tmap, trans);
  }

  private void build(TemplateMap tmap, Vector<Transition> trans) {
  	this.trans = trans;
    this.tmap = tmap;
  }

  public Vector<Transition> getTransitions() {
    return trans;
  }

  public TemplateMap getTemplateMap() {
    return tmap;
  }

  public boolean evaluate(HashMap<String,Rational> vals) {
    boolean ans = tmap.evaluate(vals);
    return ans;
  }

}