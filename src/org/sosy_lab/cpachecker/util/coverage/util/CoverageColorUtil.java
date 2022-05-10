// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.util;

import com.google.common.collect.Multiset;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class used for all measures and TDCGs to specify the coverage color. The color is
 * typically used for the report.html - CFA, Source, TDCG Tab.
 */
public class CoverageColorUtil {
  public static final String DEFAULT_COVERAGE_COLOR = "#3aec49";
  public static final String DEFAULT_CONSIDERED_COLOR = "#ff6e6e";
  public static final String DEFAULT_ELEMENT_COLOR = "#ffffff";

  public static final String GREEN_TDCG_COLOR = "#3cc220";
  public static final String BLUE_TDCG_COLOR = "#1a81d5";
  public static final String RED_TDCG_COLOR = "#e33636";
  public static final String YELLOW_TDCG_COLOR = "#d9ae19";
  public static final String PURPLE_TDCG_COLOR = "#7c0eb4";

  public static final String LIGHT_BLUE_LINE_COLOR = "#b9e4fa";
  public static final String DARK_BLUE_LINE_COLOR = "#94dbff";

  public static final String MAX_GRADIENT_COLOR = "#1d5e07";
  public static final String MIN_GRADIENT_COLOR = "#beffb3";

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
  public static Map<Integer, String> getFrequencyColorMap(Multiset<Integer> locations) {
    Map<Integer, String> frequencyColorMap = new HashMap<>();
    for (Integer location : locations.elementSet()) {
      String gradientColor = chooseColorFromGradient(getFrequencyValueMap(locations).get(location));
      frequencyColorMap.put(location, gradientColor);
    }
    return frequencyColorMap;
  }

  private static Map<Integer, Float> getFrequencyValueMap(Multiset<Integer> locations) {
    Map<Integer, Float> frequencyMap = new HashMap<>();
    for (Integer location : locations.elementSet()) {
      frequencyMap.put(
          location, locations.count(location) / (float) getMaxFrequencyCount(locations));
    }
    return frequencyMap;
  }

  private static int getMaxFrequencyCount(Multiset<Integer> locations) {
    int max = 0;
    for (var x : locations.elementSet()) {
      int candidate = locations.count(x);
      if (candidate > max) {
        max = candidate;
      }
    }
    return max;
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
