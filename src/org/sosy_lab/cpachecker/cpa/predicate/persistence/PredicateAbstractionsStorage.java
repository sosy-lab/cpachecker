// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.Converter;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.FormulaParser;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateAbstractionsStorage {

  public static class AbstractionNode {
    private final int id;
    private final OptionalInt locationId;
    private final BooleanFormula formula;

    public AbstractionNode(int pId, BooleanFormula pFormula, OptionalInt pLocationId) {
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

    public OptionalInt getLocationId() {
      return locationId;
    }

    @Override
    public String toString() {
      return Integer.toString(getId());
    }

    // TODO: equals() and hashCode()?
  }

  private static final Pattern NODE_DECLARATION_PATTERN = Pattern.compile("^[0-9]+[ ]*\\(([0-9]+[,]*)*\\)[ ]*(@[0-9]+)$");
  private enum AbstractionsParserState {EXPECT_OF_COMMON_DEFINITIONS, EXPECT_NODE_DECLARATION, EXPECT_NODE_ABSTRACTION}

  private final Path abstractionsFile;
  private final FormulaManagerView fmgr;
  protected final LogManager logger;
  private final Converter converter;

  private Integer rootAbstractionId = null;
  private ImmutableMap<Integer, AbstractionNode> abstractions = ImmutableMap.of();
  private ImmutableListMultimap<Integer, Integer> abstractionTree = ImmutableListMultimap.of();
  private Set<Integer> reusedAbstractions = new TreeSet<>();

  public PredicateAbstractionsStorage(Path pFile, LogManager pLogger, FormulaManagerView pFmgr, @Nullable Converter pConverter) throws PredicateParsingFailedException {
    this.fmgr = pFmgr;
    this.logger = pLogger;
    this.abstractionsFile = pFile;
    this.converter = pConverter;

    if (pFile != null) {
      try {
        IO.checkReadableFile(pFile);
        parseAbstractionTree();
      } catch (IOException e) {
        throw new PredicateParsingFailedException(e, "Init", 0);
      }
    }
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private void parseAbstractionTree() throws IOException, PredicateParsingFailedException {
    Multimap<Integer, Integer> resultTree = LinkedHashMultimap.create();
    Map<Integer, AbstractionNode> resultAbstractions = new TreeMap<>();
    Set<Integer> abstractionsWithParents = new TreeSet<>();

    String source = abstractionsFile.getFileName().toString();
    try (BufferedReader reader =
        Files.newBufferedReader(abstractionsFile, StandardCharsets.US_ASCII)) {

      // first, read first section with initial set of function definitions
      Pair<Integer, String> defParsingResult = PredicatePersistenceUtils.parseCommonDefinitions(reader, abstractionsFile.toString());
      int lineNo = defParsingResult.getFirst();
      String commonDefinitions = convert(defParsingResult.getSecond());

      String currentLine;
      int currentAbstractionId = -1;
      OptionalInt currentLocationId = OptionalInt.empty();
      Set<Integer> currentSuccessors = new TreeSet<>();

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
            if (!token.isEmpty()) {
              if (token.startsWith("@")) {
                currentLocationId = OptionalInt.of(Integer.parseInt(token.substring(1)));
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

          currentLine = convert(currentLine);

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
    this.abstractionTree = ImmutableListMultimap.copyOf(resultTree);
  }

  private String convert(String str) {
    if (converter == null){
      return str;
    }

    LogManagerWithoutDuplicates logger2 = new LogManagerWithoutDuplicates(logger);
    StringBuilder out = new StringBuilder();
    for (String line : Splitter.on('\n').split(str)) {
      line = FormulaParser.convertFormula(checkNotNull(converter), line, logger2);
      if (line != null) {
        out.append(line).append("\n");
      }
    }

    return out.toString();
  }

  public AbstractionNode getAbstractionNode(int abstractionId) {
    return abstractions.get(abstractionId);
  }

  public ImmutableListMultimap<Integer, Integer> getAbstractionTree() {
    return abstractionTree;
  }

  public Integer getRootAbstractionId() {
    return rootAbstractionId;
  }

  public Set<AbstractionNode> getSuccessorAbstractions(Integer ofAbstractionWithId) {
    Set<AbstractionNode> result = new LinkedHashSet<>();

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
