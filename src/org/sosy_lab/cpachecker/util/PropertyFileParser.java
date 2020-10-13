// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonCoverageType;
import org.sosy_lab.cpachecker.core.specification.Property.CommonPropertyType;
import org.sosy_lab.cpachecker.cpa.testtargets.CoverFunction;
import org.sosy_lab.cpachecker.util.ltl.LtlParseException;
import org.sosy_lab.cpachecker.util.ltl.LtlParser;

/**
 * A simple class that reads a property, i.e. basically an entry function and a proposition, from a given property,
 * and maps the proposition to a file from where to read the specification automaton.
 */
public class PropertyFileParser {

  public static class InvalidPropertyFileException extends Exception {

    private static final long serialVersionUID = -5880923544560903123L;

    public InvalidPropertyFileException(String msg) {
      super(msg);
    }

    public InvalidPropertyFileException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  private final Path propertyFile;

  private String entryFunction;
  private final Set<Property> properties = Sets.newHashSetWithExpectedSize(1);

  private static final Pattern PROPERTY_PATTERN =
      Pattern.compile("CHECK\\( init\\((" + CFACreator.VALID_C_FUNCTION_NAME_PATTERN + ")\\(\\)\\), LTL\\((.+)\\) \\)");

  private static final Pattern COVERAGE_PATTERN =
      Pattern.compile(
          "COVER\\( init\\(("
              + CFACreator.VALID_C_FUNCTION_NAME_PATTERN
              + ")\\(\\)\\), FQL\\((.+)\\) \\)");

  private static final ImmutableMap<String, ? extends Property> AVAILABLE_VERIFICATION_PROPERTIES =
      Maps.uniqueIndex(EnumSet.allOf(CommonPropertyType.class), Property::toString);

  private static final ImmutableMap<String, ? extends Property> AVAILABLE_COVERAGE_PROPERTIES =
      Maps.uniqueIndex(EnumSet.allOf(CommonCoverageType.class), Property::toString);

  public PropertyFileParser(final Path pPropertyFile) {
    propertyFile = pPropertyFile;
  }

  public void parse() throws InvalidPropertyFileException, IOException {
    String rawProperty = null;
    try (BufferedReader br = Files.newBufferedReader(propertyFile, Charset.defaultCharset())) {
      while ((rawProperty = br.readLine()) != null) {
        if (!rawProperty.isEmpty()) {
          properties.add(parsePropertyLine(rawProperty));
        }
      }
    }
    if (properties.isEmpty()) {
      throw new InvalidPropertyFileException("No property in file.");
    }
  }

  private Property parsePropertyLine(String rawProperty) throws InvalidPropertyFileException {
    Matcher matcher = PROPERTY_PATTERN.matcher(rawProperty);

    if (rawProperty == null) {
      throw new InvalidPropertyFileException("The property is not well-formed!");
    }

    Map<String, ? extends Property> propStringToProperty = AVAILABLE_VERIFICATION_PROPERTIES;

    if (!matcher.matches() || matcher.groupCount() != 2) {
      matcher = COVERAGE_PATTERN.matcher(rawProperty);
      if (!matcher.matches() || matcher.groupCount() != 2) {
        throw new InvalidPropertyFileException(
            String.format("The property '%s' is not well-formed!", rawProperty));
      }
      propStringToProperty = AVAILABLE_COVERAGE_PROPERTIES;
    }

    if (entryFunction == null) {
      entryFunction = matcher.group(1);
    } else if (!entryFunction.equals(matcher.group(1))) {
      throw new InvalidPropertyFileException(String.format(
          "Specifying two different entry functions %s and %s is not supported.", entryFunction, matcher.group(1)));
    }

    String rawLtlProperty = matcher.group(2);
    Property property = propStringToProperty.get(rawLtlProperty);
    if (property == null && propStringToProperty == AVAILABLE_VERIFICATION_PROPERTIES) {
      try {
        property = LtlParser.parseProperty(rawLtlProperty);
      } catch (LtlParseException e) {
        throw new InvalidPropertyFileException(
            String.format("Could not parse property '%s' (%s)", matcher.group(2), e.getMessage()),
            e);
      }
    }
    if (property == null && propStringToProperty == AVAILABLE_COVERAGE_PROPERTIES) {
      property = CoverFunction.getProperty(rawProperty);
    }
    return property;
  }

  public String getEntryFunction() {
    return entryFunction;
  }

  public Set<Property> getProperties() {
    return Collections.unmodifiableSet(properties);
  }
}