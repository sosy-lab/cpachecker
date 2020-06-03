/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.loopacceleration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

// @Options(prefix = "cpa.loopIO")
public class LoopInputOutputIdea {

  private CFA cfa;
  private Optional<LoopStructure> ls;
  private ImmutableList<ImmutableList<CFAEdge>> edge = ImmutableList.<ImmutableList<CFAEdge>>builder().build();
  private ImmutableSet<CFANode> head;

  private ImmutableList <String> input = ImmutableList.<String>builder().build();
  private ImmutableList <String> output = ImmutableList.<String>builder().build();


  public LoopInputOutputIdea(CFA pCfa, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    cfa = pCfa;
  }

  private void findAllLoops() {
    ls = cfa.getLoopStructure();
  }

  private void loopInnerEdges() {
    for(Loop l:ls.get().getAllLoops()) {
      edge.add(l.getInnerLoopEdges().asList());
    }
  }

  private void inputVariables() {
    for(ImmutableList<CFAEdge> il: edge) {
      for(CFAEdge ca : il) {
        if(ca.getEdgeType().equals(CFAEdgeType.StatementEdge)) {
          input.add(ca.getCode());
        }
      }
    }
  }

  private void loopHeads() {
    head = cfa.getAllLoopHeads().get();
  }

  private void nextNode() {
   for(CFANode h : head) {
     for(int i = 0; i <= h.getNumLeavingEdges(); i++) {
        if (!(head.asList().get(0).equals(h.getLeavingEdge(i).getSuccessor()))) {
         for(String inp : input) {
         if(h.getLeavingSummaryEdge().getCode().contains(inp)) {
           output.add(inp);
         }
          }
       }
     }
    }
   }
}

