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
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;

public class AutomatonWitnessV2ParserUtils {

  public static class InvalidYAMLWitnessException extends InvalidConfigurationException {
    private static final long serialVersionUID = -5647551194742587246L;

    public InvalidYAMLWitnessException(String pReason) {
      super(pReason);
    }
  }

  /**
   * Parses a YAML file and returns the entries found in the file.
   *
   * @param pInputStream the input stream to parse the YAML contents from.
   * @return the entries found in the file.
   * @throws IOException if there occurs an IOException while reading from the stream.
   */
  public static List<AbstractEntry> parseYAML(InputStream pInputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    return Arrays.asList(mapper.readValue(pInputStream, AbstractEntry[].class));
  }

  /**
   * Determine the scope for the given line in the source code.
   *
   * @param pExplicitScope The explicit scope to use if present.
   * @param pFunctionStack The function stack to use if no explicit scope is present.
   * @param pLine The line for which to determine the scope.
   * @param pScope The scope to use if no explicit scope or function stack is present.
   * @return The scope for the given line in the source code.
   */
  public static Scope determineScopeForLine(
      Optional<String> pExplicitScope, Deque<String> pFunctionStack, Integer pLine, Scope pScope) {
    LineMatcher lineMatcher = new LineMatcher(Optional.empty(), pLine, pLine);
    return determineScope(pExplicitScope, pFunctionStack, lineMatcher, pScope);
  }

  private static Scope determineScope(
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
   * Determine if the given file is a YAML witness. Be aware that this parses the file into YAML
   * entries.
   *
   * @param pPath The file to check.
   * @return True if the file is a YAML witness, false otherwise.
   * @throws InvalidConfigurationException If the file is not a valid YAML witness.
   * @throws InterruptedException If the parsing is interrupted.
   */
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

  /**
   * Determine the type of the witness if it is a YAML witness. Be aware that this parses the file
   * into YAML entries.
   *
   * @param pPath The file to check.
   * @return The type of the witness if it is a YAML witness, empty otherwise.
   * @throws InterruptedException If the parsing is interrupted.
   */
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

  static Optional<WitnessType> getWitnessTypeIfYAML(List<AbstractEntry> entries) {
    if (FluentIterable.from(entries).allMatch(e -> e instanceof ViolationSequenceEntry)) {
      return Optional.of(WitnessType.VIOLATION_WITNESS);
    } else if (FluentIterable.from(entries).allMatch(e -> !(e instanceof ViolationSequenceEntry))) {
      return Optional.of(WitnessType.CORRECTNESS_WITNESS);
    }
    return Optional.empty();
  }
}
