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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class PredicateAbstractionReuse {

  public static class StateNode {
    private final int id;
    private final Optional<Integer> locationId;

    public StateNode(int pId, Optional<Integer> pLocationId) {
      this.id = pId;
      this.locationId = pLocationId;
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
      return super.hashCode();
    }
  }

  public static class AbstractionNode extends StateNode {
    private final BooleanFormula formula;

    public AbstractionNode(int pId, BooleanFormula pFormula, Optional<Integer> pLocationId) {
      super(pId, pLocationId);
      this.formula = pFormula;
    }

    public BooleanFormula getFormula() {
      return formula;
    }
  }

  private static final Pattern NODE_DECLARATION_PATTERN = Pattern.compile("^[0-9]+[ ]*\\(([0-9]+[,]*)*\\)[ ]*(@[0-9]+)$");
  private enum AbstractionsParserState {EXPECT_OF_COMMON_DEFINITIONS, EXPECT_NODE_DECLARATION, EXPECT_NODE_DEFINITION}

  private final Path abstractionsFile;
  private final FormulaManagerView fmgr;
  protected final LogManager logger;

  private Integer rootAbstractionId = null;
  private Map<Integer, StateNode> stateNodes = Collections.emptyMap();
  private ImmutableMultimap<Integer, Integer> stateTree = ImmutableMultimap.of();
  private Set<Integer> reusedAbstractions = Sets.newTreeSet();

  public PredicateAbstractionReuse(Path pFile, LogManager pLogger, FormulaManagerView pFmgr) throws PredicateParsingFailedException, InvalidConfigurationException {
    this.fmgr = pFmgr;
    this.logger = pLogger;
    this.abstractionsFile = pFile;

    if (pFile != null) {
      try {
        Files.checkReadableFile(pFile);
        parseStateTree();
      } catch (IOException e) {
        throw new PredicateParsingFailedException(e, "Init", 0);
      }
    }
  }

  private void parseStateTree() throws IOException, PredicateParsingFailedException {
    Multimap<Integer, Integer> resultTree = LinkedHashMultimap.create();
    Map<Integer, StateNode> resultStates = Maps.newTreeMap();
    Set<Integer> statesWithParents = Sets.newTreeSet();

    String source = abstractionsFile.getFileName().toString();
    try (BufferedReader reader = java.nio.file.Files.newBufferedReader(abstractionsFile, Charsets.US_ASCII)) {

      // first, read first section with initial set of function definitions
      Pair<Integer, String> defParsingResult = PredicatePersistenceUtils.parseCommonDefinitions(reader, abstractionsFile.toString());
      int lineNo = defParsingResult.getFirst();
      String commonDefinitions = defParsingResult.getSecond();

      String currentLine;
      int currentStateId = -1;
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
          currentStateId = Integer.parseInt(declarationTokenizer.nextToken());
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

          parserState = AbstractionsParserState.EXPECT_NODE_DEFINITION;

        } else if (parserState == AbstractionsParserState.EXPECT_NODE_DEFINITION) {
          if (!currentLine.startsWith("(") && currentLine.endsWith(")")) {
            throw new PredicateParsingFailedException("unexpected line " + currentLine, source, lineNo);
          }

          StateNode stateNode = null;

          if (currentLine.startsWith("(assert ")) {
            try {
              BooleanFormula f = fmgr.parse(commonDefinitions + currentLine);
              stateNode = new AbstractionNode(currentStateId, f, currentLocationId);
            } catch (IllegalArgumentException e) {
              throw new PredicateParsingFailedException(e, "Formula parsing", lineNo);
            }
          } else {
            stateNode = new StateNode(currentStateId, currentLocationId);
          }

          resultStates.put(currentStateId, stateNode);
          resultTree.putAll(currentStateId, currentSuccessors);
          statesWithParents.addAll(currentSuccessors);
          currentStateId = -1;
          currentSuccessors.clear();

          parserState = AbstractionsParserState.EXPECT_NODE_DECLARATION;
        }
      }
    }

    // Determine root node
    Set<Integer> nodesWithNoParents = Sets.difference(resultStates.keySet(), statesWithParents);
    assert nodesWithNoParents.size() <= 1;
    if (!nodesWithNoParents.isEmpty()) {
      this.rootAbstractionId = nodesWithNoParents.iterator().next();
    } else {
      this.rootAbstractionId = null;
    }

    // Set results
    this.stateNodes = Collections.unmodifiableMap(resultStates);
    this.stateTree = ImmutableMultimap.copyOf(resultTree);
  }

  public StateNode getStateNode(int stateId) {
    return stateNodes.get(stateId);
  }

  public AbstractionNode getAbstractionNode(int stateId) {
    StateNode sn = stateNodes.get(stateId);
    if (!(sn instanceof AbstractionNode)) {
      throw new IllegalStateException("Queried state is not an abstraction state!");
    }

    return (AbstractionNode) sn;
  }

  public ImmutableMultimap<Integer, Integer> getAbstractionTree() {
    return stateTree;
  }

  public int getRootAbstractionId() {
    Preconditions.checkNotNull(rootAbstractionId);
    return rootAbstractionId;
  }

  public Set<Integer> getSuccessorStateIds(Integer ofStateWithId, Optional<Integer> filterByLocationId) {
    Set<Integer> result = Sets.newHashSet();

    for (StateNode an: getSuccessorStates(ofStateWithId, filterByLocationId)) {
      result.add(an.getId());
    }

    return result;
  }

  public Set<AbstractionNode> getSuccessorAbstractions(Integer ofStateWithId, Optional<Integer> filterByLocationId) {
    Set<AbstractionNode> result = Sets.newHashSet();

    for (StateNode sn: getSuccessorStates(ofStateWithId, filterByLocationId)) {
      if (sn instanceof AbstractionNode) {
        result.add((AbstractionNode) sn);
      }
    }

    return result;
  }

  public Set<StateNode> getSuccessorStates(Integer ofStateWithId, Optional<Integer> filterByLocationId) {
    Set<StateNode> result = Sets.newHashSet();

    if (stateTree != null) {
      for (Integer successorId : stateTree.get(ofStateWithId)) {
        if (successorId == null) {
          continue;
        }

        StateNode successor = stateNodes.get(successorId);
        if (successor == null) {
          continue;
        }

        if (filterByLocationId.isPresent()) {
          if (successor.getLocationId().isPresent()) {
            if (!filterByLocationId.get().equals(successor.getLocationId().get())) {
              continue;
            }
          }
        }

        result.add(successor);
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


  public Collection<AbstractionNode> getAbstractionNodes() {
    Collection<AbstractionNode> result = Sets.newHashSet();
    for (StateNode sn: stateNodes.values()) {
      if (sn instanceof AbstractionNode) {
        result.add((AbstractionNode) sn);
      }
    }
    return result;
  }

  public Collection<StateNode> getStateNodes() {
    return stateNodes.values();
  }

  public boolean hasDataForReuse() {
    return this.abstractionsFile != null;
  }


}
