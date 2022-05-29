// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import java.awt.Color;
import java.util.Comparator;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Utility class used for all measures and TDCGs to specify the coverage color. The color is
 * typically used for the report.html - CFA, Source, TDCG Tab.
 */
public class CoverageColorUtil {
  public static final String DEFAULT_COVERAGE_COLOR = "#1eff00";
  public static final String DEFAULT_CONSIDERED_COLOR = "#ff6e6e";
  public static final String DEFAULT_ELEMENT_COLOR = "#ffffff";
  public static final String DEFAULT_RELEVANT_VARIABLE_COLOR = "#ff2424";

  public static final String GREEN_TDCG_COLOR = "#3cc220";
  public static final String BLUE_TDCG_COLOR = "#1a81d5";
  public static final String RED_TDCG_COLOR = "#e33636";
  public static final String YELLOW_TDCG_COLOR = "#d9ae19";

  public static final String LIGHT_BLUE_LINE_COLOR = "#b9e4fa";
  public static final String DARK_BLUE_LINE_COLOR = "#94dbff";

  public static final String MAX_GRADIENT_COLOR = "#15a602";
  public static final String MIN_GRADIENT_COLOR = "#beffb5";

  /**
   * Returns for a given line a color representation, depending on if the line number is even or
   * odd.
   *
   * @param lineNumber line number from source code.
   * @return hex color code for the given line.
   */
  public static String getAlternatingLineColor(int lineNumber) {
    if (lineNumber % 2 == 0) {
      return LIGHT_BLUE_LINE_COLOR;
    } else {
      return DARK_BLUE_LINE_COLOR;
    }
  }

  /**
   * Returns a color heat map for every location, calculated from a location multiset. The color
   * representation is a gradient between two given colors, depending on the frequency of the
   * coverage status given in the location multiset.
   *
   * @param locations locations is a multiset of CFA locations which are marked as covered.
   * @return color heat map for every location, where the color is defined as hex color code.
   */
  public static Map<CFANode, String> getFrequencyColorMapForLocations(Multiset<CFANode> locations) {
    Multiset<Integer> locationIds =
        locations.stream()
            .map(l -> l.getNodeNumber())
            .collect(ImmutableMultiset.toImmutableMultiset());
    Map<Integer, Float> frequencyValueMap = getFrequencyValueMap(locationIds);
    return locations.elementSet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                l -> l, l -> chooseColorFromGradient(frequencyValueMap.get(l.getNodeNumber()))));
  }

  /**
   * Returns a color heat map for every source code line, calculated from a line multiset. The color
   * representation is a gradient between two given colors, depending on the frequency of the
   * coverage status given in the line multiset.
   *
   * @param lines lines is a multiset of source code lines which are marked as covered.
   * @return color heat map for every line, where the color is defined as hex color code.
   */
  public static Map<Integer, String> getFrequencyColorMapForLines(Multiset<Integer> lines) {
    Map<Integer, Float> frequencyValueMap = getFrequencyValueMap(lines);
    return lines.elementSet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                l -> l, l -> chooseColorFromGradient(frequencyValueMap.get(l))));
  }

  private static Map<Integer, Float> getFrequencyValueMap(Multiset<Integer> locations) {
    int maxFrequencyCount = getMaxFrequencyCount(locations);
    return locations.elementSet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                l -> l, l -> locations.count(l) / (float) maxFrequencyCount));
  }

  private static int getMaxFrequencyCount(Multiset<Integer> locations) {
    return locations.elementSet().stream()
        .map(l -> locations.count(l))
        .max(Comparator.naturalOrder())
        .orElse(0);
  }

  private static String chooseColorFromGradient(float gradient) {
    Color color1 = Color.decode(MAX_GRADIENT_COLOR);
    Color color2 = Color.decode(MIN_GRADIENT_COLOR);
    Color rgbGradient = colorGradient(color1, color2, gradient);

    return rgbToHex(rgbGradient);
  }

  private static Color colorGradient(Color color1, Color color2, float weight) {
    float inverseWeight = 1 - weight;
    return new Color(
        Math.round(color1.getRed() * weight + color2.getRed() * inverseWeight),
        Math.round(color1.getGreen() * weight + color2.getGreen() * inverseWeight),
        Math.round(color1.getBlue() * weight + color2.getBlue() * inverseWeight));
  }

  private static String rgbToHex(Color color) {
    String hex = Integer.toHexString(color.getRGB() & 0xffffff);
    if (hex.length() < 6) {
      hex = "0" + hex;
    }
    return "#" + hex;
  }
}
