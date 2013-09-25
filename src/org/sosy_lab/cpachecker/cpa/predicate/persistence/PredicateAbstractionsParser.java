/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;

import org.sosy_lab.common.Files;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;


public class PredicateAbstractionsParser {

  public static class AbstractionNode {
    private final int id;
    private final BooleanFormula formula;

    public AbstractionNode(int pId, BooleanFormula pFormula) {
      this.id = pId;
      this.formula = pFormula;
    }

    public BooleanFormula getFormula() {
      return formula;
    }

    public int getId() {
      return id;
    }
  }

  private final Path inputFilePath;
  private AbstractionNode rootAbstractionNode = null;
  private ImmutableMultimap<AbstractionNode, AbstractionNode> abstractionTree = null;

  public PredicateAbstractionsParser(Path pInputFilePath) throws IOException {
    this.inputFilePath = pInputFilePath;
    Files.checkReadableFile(inputFilePath);
    parseAbstractionTree();
  }

  private void parseAbstractionTree() throws IOException {
    Multimap<AbstractionNode, AbstractionNode> result = LinkedHashMultimap.create();

    try (BufferedReader reader = java.nio.file.Files.newBufferedReader(inputFilePath, Charsets.US_ASCII)) {
    }

    this.rootAbstractionNode = null;
    this.abstractionTree = ImmutableMultimap.copyOf(result);
  }

  public synchronized ImmutableMultimap<AbstractionNode, AbstractionNode> getAbstractionTree() {
    assert abstractionTree != null;
    return abstractionTree;
  }

  public synchronized AbstractionNode getRootAbstractionNode() {
    assert rootAbstractionNode != null;
    return rootAbstractionNode;
  }

}
