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
package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class PredicateAbstractionsStorage {

  public static class AbstractionNode {
    private final int id;
    private final Optional<Integer> locationId;
    private final BooleanFormula formula;

    public AbstractionNode(int pId, BooleanFormula pFormula, Optional<Integer> pLocationId) {
      this.id = pId;
      this.formula = pFormula;
      this.locationId = pLocationId;
    }

    public BooleanFormula getFormula() {
      return formula;
    }

    public int getId() {
      return id;
    }

    public Optional<Integer> getLocationId() {
      return locationId;
    }

    @Override
    public String toString() {
      return Integer.toString(getId());
    }

    @Override
    public int hashCode() {
      // TODO
      return super.hashCode();
    }
  }

  private static final Pattern NODE_DECLARATION_PATTERN = Pattern.compile("^[0-9]+[ ]*\\(([0-9]+[,]*)*\\)[ ]*(@[0-9]+)$");
  private enum AbstractionsParserState {EXPECT_OF_COMMON_DEFINITIONS, EXPECT_NODE_DECLARATION, EXPECT_NODE_ABSTRACTION}

  private final Path abstractionsFile;
  private final FormulaManagerView fmgr;
  protected final LogManager logger;

  private Integer rootAbstractionId = null;
  private ImmutableMap<Integer, AbstractionNode> abstractions = ImmutableMap.of();
  private ImmutableMultimap<Integer, Integer> abstractionTree = ImmutableMultimap.of();
  private Set<Integer> reusedAbstractions = Sets.newTreeSet();

  public PredicateAbstractionsStorage(Path pFile, LogManager pLogger, FormulaManagerView pFmgr) throws PredicateParsingFailedException, InvalidConfigurationException {
    this.fmgr = pFmgr;
    this.logger = pLogger;
    this.abstractionsFile = pFile;

    if (pFile != null) {
      try {
        Files.checkReadableFile(pFile);
        parseAbstractionTree();
      } catch (IOException e) {
        throw new PredicateParsingFailedException(e, "Init", 0);
      }
    }
  }

  private void parseAbstractionTree() throws IOException, PredicateParsingFailedException {
    Multimap<Integer, Integer> resultTree = LinkedHashMultimap.create();
    Map<Integer, AbstractionNode> resultAbstractions = Maps.newTreeMap();
    Set<Integer> abstractionsWithParents = Sets.newTreeSet();

    String source = abstractionsFile.getName();
    try (BufferedReader reader = abstractionsFile.asCharSource(Charsets.US_ASCII).openBufferedStream()) {

      // first, read first section with initial set of function definitions
      Pair<Integer, String> defParsingResult = PredicatePersistenceUtils.parseCommonDefinitions(reader, abstractionsFile.toString());
      int lineNo = defParsingResult.getFirst();
      String commonDefinitions = defParsingResult.getSecond();

      String currentLine;
      int currentAbstractionId = -1;
      Optional<Integer> currentLocationId = Optional.absent();
      Set<Integer> currentSuccessors = Sets.newTreeSet();

      AbstractionsParserState parserState = AbstractionsParserState.EXPECT_NODE_DECLARATION;
      while ((currentLine = reader.readLine()) != null) {
        lineNo++;
        currentLine = currentLine.trim();

        if (currentLine.isEmpty()) {
          // blank lines separates sections
          continue;
        }

        if (currentLine.startsWith("//")) {
          // comment
          continue;
        }

        if (parserState == AbstractionsParserState.EXPECT_NODE_DECLARATION) {
          // we expect a new section header
          if (!currentLine.endsWith(":")) {
            throw new PredicateParsingFailedException(currentLine + " is not a valid abstraction header", source, lineNo);
          }

          currentLine = currentLine.substring(0, currentLine.length()-1).trim(); // strip off ":"
          if (currentLine.isEmpty()) {
            throw new PredicateParsingFailedException("empty header is not allowed", source, lineNo);
          }

          if (!NODE_DECLARATION_PATTERN.matcher(currentLine).matches()) {
            throw new PredicateParsingFailedException(currentLine + " is not a valid abstraction header", source, lineNo);
          }

          currentLocationId = null;
          StringTokenizer declarationTokenizer = new StringTokenizer(currentLine, " (,):");
          currentAbstractionId = Integer.parseInt(declarationTokenizer.nextToken());
          while (declarationTokenizer.hasMoreTokens()) {
            String token = declarationTokenizer.nextToken().trim();
            if (token.length() > 0) {
              if (token.startsWith("@")) {
                currentLocationId = Optional.of(Integer.parseInt(token.substring(1)));
              } else {
                int successorId = Integer.parseInt(token);
                currentSuccessors.add(successorId);
              }
            }
          }

          parserState = AbstractionsParserState.EXPECT_NODE_ABSTRACTION;

        } else if (parserState == AbstractionsParserState.EXPECT_NODE_ABSTRACTION) {
          if (!currentLine.startsWith("(assert ") && currentLine.endsWith(")")) {
            throw new PredicateParsingFailedException("unexpected line " + currentLine, source, lineNo);
          }

          BooleanFormula f;
          try {
            f = fmgr.parse(commonDefinitions + currentLine);
          } catch (IllegalArgumentException e) {
            throw new PredicateParsingFailedException(e, "Formula parsing", lineNo);
          }

          AbstractionNode abstractionNode = new AbstractionNode(currentAbstractionId, f, currentLocationId);
          resultAbstractions.put(currentAbstractionId, abstractionNode);
          resultTree.putAll(currentAbstractionId, currentSuccessors);
          abstractionsWithParents.addAll(currentSuccessors);
          currentAbstractionId = -1;
          currentSuccessors.clear();

          parserState = AbstractionsParserState.EXPECT_NODE_DECLARATION;
        }
      }
    }

    // Determine root node
    Set<Integer> nodesWithNoParents = Sets.difference(resultAbstractions.keySet(), abstractionsWithParents);
    assert nodesWithNoParents.size() <= 1;
    if (!nodesWithNoParents.isEmpty()) {
      this.rootAbstractionId = nodesWithNoParents.iterator().next();
    } else {
      this.rootAbstractionId = null;
    }

    // Set results
    this.abstractions = ImmutableMap.copyOf(resultAbstractions);
    this.abstractionTree = ImmutableMultimap.copyOf(resultTree);
  }

  public AbstractionNode getAbstractionNode(int abstractionId) {
    return abstractions.get(abstractionId);
  }

  public ImmutableMultimap<Integer, Integer> getAbstractionTree() {
    return abstractionTree;
  }

  public Integer getRootAbstractionId() {
    return rootAbstractionId;
  }

  public Set<AbstractionNode> getSuccessorAbstractions(Integer ofAbstractionWithId) {
    Set<AbstractionNode> result = Sets.newHashSet();

    if (abstractionTree != null) {
      for (Integer successorId : abstractionTree.get(ofAbstractionWithId)) {
        if (successorId == null) {
          continue;
        }
        AbstractionNode successor = abstractions.get(successorId);
        if (successor != null) {
          result.add(successor);
        }
      }
    }

    return result;
  }

  public void markAbstractionBeingReused(Integer abstractionId) {
    Preconditions.checkNotNull(abstractionId);
    reusedAbstractions.add(abstractionId);
  }

  public boolean wasAbstractionReused(Integer abstractionId) {
    Preconditions.checkNotNull(abstractionId);
    return reusedAbstractions.contains(abstractionId);
  }


  public ImmutableMap<Integer, AbstractionNode> getAbstractions() {
    return abstractions;
  }

}
