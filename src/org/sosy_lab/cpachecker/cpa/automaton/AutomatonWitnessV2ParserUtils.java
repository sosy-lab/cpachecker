// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.SourceLocationMatcher.LineMatcher;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.ViolationSequenceEntry;

public class AutomatonWitnessV2ParserUtils {

  public static List<AbstractEntry> parseYAML(InputStream pInputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    return Arrays.asList(mapper.readValue(pInputStream, AbstractEntry[].class));
  }

  public static Scope determineScopeForLine(
      Optional<String> pExplicitScope, Deque<String> pFunctionStack, Integer pLine, Scope pScope) {
    LineMatcher lineMatcher = new LineMatcher(Optional.empty(), pLine, pLine);
    return determineScope(pExplicitScope, pFunctionStack, lineMatcher, pScope);
  }

  public static Scope determineScope(
      Optional<String> pExplicitScope,
      Deque<String> pFunctionStack,
      Predicate<FileLocation> pLocationDescriptor,
      Scope pScope) {
    Scope result = pScope;
    if (result instanceof CProgramScope r) {
      result = r.withLocationDescriptor(pLocationDescriptor);
      if (pExplicitScope.isPresent() || !pFunctionStack.isEmpty()) {
        final String functionName;
        if (pExplicitScope.isPresent()) {
          functionName = pExplicitScope.orElseThrow();
        } else {
          functionName = pFunctionStack.peek();
        }
        result = r.withFunctionScope(functionName);
      }
    }
    return result;
  }

  /**
   * Returns the offsets for the given file. If the file is not found the offsets for the file with
   * the largest matching suffix will be returned.
   *
   * @param pOffsetsByFile The Map containing the currently known offsets by file. This map will be
   *     updated if the file is not found with the best known match.
   * @param pFile The file for which to get the offsets.
   * @return The offsets for the given file. If the file is not found the offsets for the file with
   *     the largest matching suffix will be returned.
   */
  public static List<Integer> getOffsetsByFileSimilarity(
      ListMultimap<String, Integer> pOffsetsByFile, String pFile) {
    String maxSimilarityFile = pFile;
    if (pOffsetsByFile.containsKey(pFile)) {
      return pOffsetsByFile.get(pFile);
    }

    // The file extension plus at least one character of the file should match
    int maxSimilarity = 0;
    for (String file : pOffsetsByFile.keySet()) {
      int similarity = 0;
      int index1 = file.length() - 1;
      int index2 = pFile.length() - 1;
      while (index1 >= 0 && index2 >= 0 && file.charAt(index1) == pFile.charAt(index2)) {
        similarity++;
        index1--;
        index2--;
      }
      if (similarity > maxSimilarity) {
        maxSimilarity = similarity;
        maxSimilarityFile = file;
      }
    }

    List<Integer> offsets = pOffsetsByFile.get(maxSimilarityFile);
    pOffsetsByFile.putAll(pFile, offsets);

    return offsets;
  }

  public static boolean isYAMLWitness(Path pPath)
      throws InvalidConfigurationException, InterruptedException {
    return AutomatonGraphmlParser.handlePotentiallyGZippedInput(
        MoreFiles.asByteSource(pPath),
        x -> {
          try {
            AutomatonWitnessV2ParserUtils.parseYAML(x);
            return true;
          } catch (JsonProcessingException e) {
            return false;
          }
        },
        WitnessParseException::new);
  }

  public static Optional<WitnessType> getWitnessTypeIfYAML(Path pPath) throws InterruptedException {
    List<AbstractEntry> entries;
    try {
      entries =
          AutomatonGraphmlParser.handlePotentiallyGZippedInput(
              MoreFiles.asByteSource(pPath),
              AutomatonWitnessV2ParserUtils::parseYAML,
              WitnessParseException::new);
    } catch (WitnessParseException e) {
      entries = ImmutableList.of();
    }
    return getWitnessTypeIfYAML(entries);
  }

  public static Optional<WitnessType> getWitnessTypeIfYAML(List<AbstractEntry> entries) {
    if (FluentIterable.from(entries).allMatch(e -> e instanceof ViolationSequenceEntry)) {
      return Optional.of(WitnessType.VIOLATION_WITNESS);
    } else if (FluentIterable.from(entries).allMatch(e -> !(e instanceof ViolationSequenceEntry))) {
      return Optional.of(WitnessType.CORRECTNESS_WITNESS);
    }
    return Optional.empty();
  }
}
