/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.wp.segkro.rules;

import java.util.List;

import org.sosy_lab.cpachecker.cpa.wp.segkro.interfaces.Rule;

import com.google.common.collect.Lists;


public class RulesetFactory {

  public static List<Rule> createRuleset() {
    List<Rule> result = Lists.newArrayList();

    // the ordering of this rules might be important!

    result.add(new EliminationRule());
    result.add(new EquivalenceRule());
    result.add(new UniverifyRule());
    result.add(new SubstitutionRule());
    result.add(new LinkRule());
    result.add(new ExistentialRule());
    result.add(new ExtendLeftRule());
    result.add(new ExtendRightRule());

    return result;
  }

}
