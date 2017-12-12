/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.Pair;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * Class for creating a pixel graphic from an
 * {@link org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet}.
 *
 * There are different options for modifying how the pixel graphic looks like.
 *
 * An ARG is represented as a tree structure.
 * The depth of an ARG node in the tree equals its shortest distance from the root of the ARG.
 */
@Options(prefix = "cpa.arg.bitmap")
public class ARGToBitmapWriter {

  @Option(secure=true, description="Padding of the bitmap on the left and right (each) in pixels")
  private int xPadding = 2;

  @Option(secure=true, description="Padding of the bitmap on the top and bottom (each) in pixels")
  private int yPadding = 2;

  @Option(secure=true, description="Width of the bitmap in pixels. If set to -1, width is computed"
      + " in relation to the height. If both are set to -1, the optimal bitmap size"
      + " to represent the ARG is used. The final width is width*scaling")
  private int width = -1;

  @Option(secure=true, description="Height of the bitmap in pixels. If set to -1, height is "
      + " computed in relation to the width. If both are set to -1, the optimal bitmap size"
      + " to represent the ARG is used. The final height is height*scaling")
  private int height = -1;

  @Option(secure=true, description="Scaling of the bitmap. If set to 1, 1 pixel represents one "
      + "ARG node. If set to 2, 2 * 2 pixels represent one ARG node, and so on.")
  private int scaling = 2;

  @Option(secure=true, description="Highlight not only corresponding ARG nodes, but background of"
      + " corresponding line, too. This may give an better overview, but also introduces more"
      + " clutter")
  private boolean strongHighlight = true;

  @Option(secure=true, description="Format to use for image output")
  private String imageFormat = "svg";

  private static final Color COLOR_BACKGROUND = Color.LIGHT_GRAY;
  private static final Color COLOR_NODE = Color.BLACK;
  private static final Color COLOR_TARGET = Color.RED;
  private static final Color COLOR_HIGHLIGHT = Color.BLUE;
  private static final Color COLOR_NOTEXPANDED = Color.ORANGE;
  private static final Color COLOR_COVERED = Color.GREEN;

  private static final String FORMAT_SVG = "svg";

  private final CanvasProvider canvasHandler;

  public ARGToBitmapWriter(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);

    if (width == 0 || height == 0) {
      throw new InvalidConfigurationException("Width and height may not be 0");
    }
    if (scaling == 0) {
      throw new InvalidConfigurationException("Scaling may not be 0");
    }

    if (imageFormat.equals(FORMAT_SVG)) {
      canvasHandler = new SvgProvider();
    } else {
      canvasHandler = new BitmapProvider(imageFormat);
    }
  }

  private ARGStructure getStructure(ARGState pRoot) {

    ARGStructure structure = new ARGStructure();

    Deque<Pair<Integer, ARGState>> worklist = new ArrayDeque<>();
    Set<ARGState> processed = new HashSet<>();

    worklist.add(Pair.of(0, pRoot));
    int oldDistance = 0;
    ARGLevel.Builder levelBuilder = ARGLevel.builder();
    while (!worklist.isEmpty()) {
      Pair<Integer, ARGState> current = checkNotNull(worklist.poll()); // FIFO for BFS order
      int currentDistance = checkNotNull(current.getFirst());

      if (oldDistance != currentDistance) {
        structure.addLevel(levelBuilder.build());
        oldDistance = currentDistance;
        levelBuilder = ARGLevel.builder();
      }

      ARGState currentNode = checkNotNull(current.getSecond());
      levelBuilder.node();

      if (currentNode.isTarget()) {
        levelBuilder.markTarget();
      }
      if (currentNode.shouldBeHighlighted()) {
        levelBuilder.markHighlight();
      }
      if (!currentNode.wasExpanded()) {
        levelBuilder.markNotExpanded();
      }
      if (currentNode.isCovered()) {
        levelBuilder.markCovered();
      }

      // add current state to set of processed states *before* checking candidates
      // - in general, self edges may exist in the ARG
      processed.add(currentNode);

      Collection<ARGState> candidates = currentNode.getChildren();
      for (ARGState s : candidates) {
        if (!processed.contains(s)) {
          worklist.add(Pair.of(currentDistance+1, s));
        }
      }
    }
    structure.addLevel(levelBuilder.build());

    return structure;
  }

  private int getWidth(ARGStructure pArgStructure) throws InvalidConfigurationException {
    int finalWidth;

    { // Create block so neededWidth can only be used for allocation
      int neededWidth = (scaling * pArgStructure.getMaxWidth()) + xPadding * 2;

      int intendedWidth = scaling * width;

      if (intendedWidth > 0) {
        if (intendedWidth < neededWidth) {
          throw new InvalidConfigurationException("ARG doesn't fit on the defined canvas. Needed "
              + "width: " + neededWidth);
        }
        finalWidth = intendedWidth;
      } else {
        finalWidth = neededWidth;
      }
    }
    return finalWidth;
  }

  private int getHeight(ARGStructure pArgStructure) throws InvalidConfigurationException {
    int finalHeight;

    { // Create block so neededHeight can only be used for allocation
      int neededHeight = (scaling * pArgStructure.getDepth()) + yPadding * 2;

      int intendedHeight = scaling * height;

      if (intendedHeight > 0) {
        if (intendedHeight < neededHeight) {
          throw new InvalidConfigurationException("ARG doesn't fit on the defined canvas. Needed "
              + "height: " + neededHeight);
        }
        finalHeight = intendedHeight;
      } else {
        finalHeight = neededHeight;
      }
    }

    return finalHeight;
  }

  private void drawContent(
      Graphics2D pCanvas, int pWidth, int pHeight, ARGStructure pArgStructure) {
    pCanvas.setColor(COLOR_BACKGROUND);
    pCanvas.fillRect(0, 0, pWidth, pHeight);

    int middle = pWidth / 2;
    int stateNum;
    int xPos;
    int yPos = yPadding;
    for (ARGLevel level : pArgStructure) {
      stateNum = level.getWidth();
      int lineWidth = stateNum * scaling;

      xPos = middle - lineWidth / 2;

      if (strongHighlight) {
        Color levelBackground = level.getBackgroundColor();
        pCanvas.setColor(levelBackground);
        pCanvas.fillRect(0, yPos, pWidth, scaling);
      }

      pCanvas.setColor(COLOR_NODE);
      pCanvas.fillRect(xPos, yPos, lineWidth, scaling);

      for (Pair<List<Integer>, Color> p : level.getGroups()) {
        pCanvas.setColor(p.getSecondNotNull());
        for (int idx : p.getFirstNotNull()) {
          pCanvas.fillRect(xPos + (idx - 1) * scaling, yPos, scaling, scaling);
        }
      }

      yPos += scaling;
    }
  }

  public void write(
      ARGState pRoot,
      Path pOutputFile
  ) throws IOException, InvalidConfigurationException {
    ARGStructure structure = getStructure(pRoot);

    int finalWidth = getWidth(structure);
    int finalHeight = getHeight(structure);

    Graphics2D g = canvasHandler.createCanvas(finalWidth, finalHeight);
    drawContent(g, finalWidth, finalHeight, structure);

    Path fullOutputFile = Paths.get(pOutputFile.toString() + "." + imageFormat);
    canvasHandler.writeToFile(fullOutputFile);
  }

  private static class ARGStructure implements Iterable<ARGLevel> {
    private List<ARGLevel> levels = new ArrayList<>();
    private int maxWidth;

    public void addLevel(ARGLevel pLevel) {
      levels.add(pLevel);
      int width = pLevel.getWidth();
      if (width > maxWidth) {
        maxWidth = pLevel.getWidth();
      }
    }

    @Override
    public Iterator<ARGLevel> iterator() {
      return levels.iterator();
    }

    public int getMaxWidth() {
      return maxWidth;
    }

    public int getDepth() {
      return levels.size();
    }
  }

  private static class ARGLevel {
    private int width;

    private List<Integer> targetIndices;
    private List<Integer> highlightIndices;
    private List<Integer> notExpandedIndices;
    private List<Integer> coveredIndices;

    private ARGLevel(
        int pWidth,
        List<Integer> pTargets,
        List<Integer> pNotExpanded,
        List<Integer> pHighlights,
        List<Integer> pCovered
    ) {
      checkArgument(pWidth >= 0);
      width = pWidth;
      targetIndices = checkNotNull(pTargets);
      notExpandedIndices = checkNotNull(pNotExpanded);
      highlightIndices = checkNotNull(pHighlights);
      coveredIndices = checkNotNull(pCovered);
    }

    static Builder builder() {
      return new Builder();
    }

    int getWidth() {
      return width;
    }

    Deque<Pair<List<Integer>, Color>> getGroups() {
      Deque<Pair<List<Integer>, Color>> groups = new ArrayDeque<>(4);
      if (!highlightIndices.isEmpty()) {
        groups.add(Pair.of(highlightIndices, COLOR_HIGHLIGHT));
      }
      if (!notExpandedIndices.isEmpty()) {
        groups.add(Pair.of(notExpandedIndices, COLOR_NOTEXPANDED));
      }
      if (!targetIndices.isEmpty()) {
        groups.add(Pair.of(targetIndices, COLOR_TARGET));
      }
      if (!coveredIndices.isEmpty()) {
        groups.add(Pair.of(coveredIndices, COLOR_COVERED));
      }

      return groups;
    }

    Color getBackgroundColor() {
      Color color;
      if (!targetIndices.isEmpty()) {
        color = COLOR_TARGET;
      } else if (!highlightIndices.isEmpty()) {
        color = COLOR_HIGHLIGHT;
      } else if (notExpandedIndices.size() > coveredIndices.size()) {
        color = COLOR_NOTEXPANDED;
      } else if (!coveredIndices.isEmpty()) {
        color = COLOR_COVERED;
      } else {
        return COLOR_BACKGROUND;
      }

      return new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
    }

    public static class Builder {

      private int width = 0;
      private ImmutableList.Builder<Integer> targets = ImmutableList.builder();
      private ImmutableList.Builder<Integer> notExpanded = ImmutableList.builder();
      private ImmutableList.Builder<Integer> highlights = ImmutableList.builder();
      private ImmutableList.Builder<Integer> covered = ImmutableList.builder();

      public ARGLevel build() {
        return new ARGLevel(
            width, targets.build(), notExpanded.build(), highlights.build(), covered.build());
      }

      @CanIgnoreReturnValue
      Builder node() {
        width++;
        return this;
      }

      @CanIgnoreReturnValue
      Builder markTarget() {
        targets.add(width);
        return this;
      }

      @CanIgnoreReturnValue
      Builder markNotExpanded() {
        notExpanded.add(width);
        return this;
      }

      @CanIgnoreReturnValue
      Builder markHighlight() {
        highlights.add(width);
        return this;
      }

      Builder markCovered() {
        covered.add(width);
        return this;
      }
    }
  }

  private interface CanvasProvider {
    Graphics2D createCanvas(int pWidth, int pHeight);

    void writeToFile(Path pOutputFile) throws IOException;
  }

  private static class BitmapProvider implements CanvasProvider {

    private String imageFormat;
    private BufferedImage bufferedImage = null;

    BitmapProvider(String pFormat) {
      imageFormat = pFormat;
    }

    @Override
    public Graphics2D createCanvas(int pWidth, int pHeight) {
      checkState(bufferedImage == null, "createCanvas can only be called after writing the old "
          + "canvas to a file");
      bufferedImage = new BufferedImage(pWidth, pHeight, BufferedImage.TYPE_3BYTE_BGR);
      return bufferedImage.createGraphics();
    }

    @Override
    public void writeToFile(Path pOutputFile) throws IOException {
      checkState(bufferedImage != null, "Canvas not created");
      try (FileImageOutputStream out = new FileImageOutputStream(pOutputFile.toFile())) {
        ImageIO.write(bufferedImage, imageFormat, out);
      }
      bufferedImage = null;
    }
  }

  private static class SvgProvider implements CanvasProvider {

    private SVGGraphics2D svgGenerator = null;

    @Override
    public Graphics2D createCanvas(int pWidth, int pHeight) {
      DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

      String svgNS = "http://www.w3.org/2000/svg";
      Document document = domImpl.createDocument(svgNS, FORMAT_SVG, null);

      // Create an instance of the SVG Generator.
      svgGenerator = new SVGGraphics2D(document);
      svgGenerator.setSVGCanvasSize(new Dimension(pWidth, pHeight));
      return svgGenerator;
    }

    @Override
    public void writeToFile(Path pOutputFile) throws IOException {
      checkState(svgGenerator != null, "Canvas not created");
      try (BufferedWriter out = Files.newBufferedWriter(pOutputFile, Charset.defaultCharset())) {
        svgGenerator.stream(out);
      }
    }
  }
}
