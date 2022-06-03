// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.pixelexport;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.primitives.ImmutableIntArray;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
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
 * Class for creating a pixel graphic from a graph like e.g. {@link org.sosy_lab.cpachecker.cfa.CFA}
 * or {@link org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet}.
 *
 * <p>There are different options for modifying how the pixel graphic looks like.
 *
 * <p>A graph is represented as a tree structure. The depth of a graph node in the tree equals to
 * its shortest distance from the root of the graph.
 */
@Options(prefix = "pixelgraphic.export")
public abstract class GraphToPixelsWriter<Node> {

  @Option(
      secure = true,
      description = "Padding of the bitmap on the left and right (each) in pixels")
  private int xPadding = 2;

  @Option(
      secure = true,
      description = "Padding of the bitmap on the top and bottom (each) in pixels")
  private int yPadding = 2;

  @Option(
      secure = true,
      description =
          "Width of the bitmap in pixels. If set to -1, width is computed"
              + " in relation to the height. If both are set to -1, the optimal bitmap size"
              + " to represent the graph is used. The final width is width*scaling")
  private int width = -1;

  @Option(
      secure = true,
      description =
          "Height of the bitmap in pixels. If set to -1, height is "
              + " computed in relation to the width. If both are set to -1, the optimal bitmap size"
              + " to represent the graph is used. The final height is height*scaling")
  private int height = -1;

  @Option(
      secure = true,
      description =
          "Scaling of the bitmap. If set to 1, 1 pixel represents one "
              + "graph node. If set to 2, 2 * 2 pixels represent one graph node, and so on.")
  private int scaling = 2;

  @Option(secure = true, description = "Format to use for image output", name = "format")
  private String imageFormat = "svg";

  @Option(
      secure = true,
      description =
          "Highlight not only corresponding graph nodes, but background of corresponding line, too."
              + " This may give an better overview, but also introduces more clutter")
  private boolean strongHighlight = true;

  public static final Color COLOR_BACKGROUND = Color.LIGHT_GRAY;
  public static final Color COLOR_NODE = Color.BLACK;

  private static final String FORMAT_SVG = "svg";

  private final CanvasProvider canvasHandler;

  protected GraphToPixelsWriter(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this, GraphToPixelsWriter.class);

    imageFormat = imageFormat.toLowerCase();

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

  public GraphStructure getStructure(Node pRoot) {

    GraphStructure structure = new GraphStructure();

    Deque<Pair<Integer, Node>> worklist = new ArrayDeque<>();
    Set<Node> reached = new HashSet<>();

    worklist.add(Pair.of(0, pRoot));
    int oldDistance = 0;
    GraphLevel.Builder<Node> levelBuilder = getLevelBuilder();
    while (!worklist.isEmpty()) {
      Pair<Integer, Node> current = checkNotNull(worklist.poll()); // FIFO for BFS order
      if (reached.contains(current.getSecond())) {
        continue;
      }
      reached.add(current.getSecond());
      int currentDistance = checkNotNull(current.getFirst());

      if (oldDistance != currentDistance) {
        structure.addLevel(levelBuilder.build());
        oldDistance = currentDistance;
        levelBuilder = getLevelBuilder();
      }

      Node currentNode = checkNotNull(current.getSecond());
      levelBuilder.node();

      levelBuilder.addMarkings(currentNode);

      for (Node s : getChildren(currentNode)) {
        if (!reached.contains(s)) {
          worklist.add(Pair.of(currentDistance + 1, s));
        }
      }
    }
    structure.addLevel(levelBuilder.build());

    return structure;
  }

  public abstract GraphLevel.Builder<Node> getLevelBuilder();

  public abstract Iterable<Node> getChildren(Node parent);

  private int getWidth(GraphStructure pGraphStructure) throws InvalidConfigurationException {
    int finalWidth;

    { // Create block so neededWidth can only be used for allocation
      int neededWidth = (scaling * pGraphStructure.getMaxWidth()) + xPadding * 2;

      int intendedWidth = scaling * width;

      if (intendedWidth > 0) {
        if (intendedWidth < neededWidth) {
          throw new InvalidConfigurationException(
              "Graph doesn't fit on the defined canvas. Needed " + "width: " + neededWidth);
        }
        finalWidth = intendedWidth;
      } else {
        finalWidth = neededWidth;
      }
    }
    return finalWidth;
  }

  private int getHeight(GraphStructure pArgStructure) throws InvalidConfigurationException {
    int finalHeight;

    { // Create block so neededHeight can only be used for allocation
      int neededHeight = (scaling * pArgStructure.getDepth()) + yPadding * 2;

      int intendedHeight = scaling * height;

      if (intendedHeight > 0) {
        if (intendedHeight < neededHeight) {
          throw new InvalidConfigurationException(
              "Graph doesn't fit on the defined canvas. Needed " + "height: " + neededHeight);
        }
        finalHeight = intendedHeight;
      } else {
        finalHeight = neededHeight;
      }
    }

    return finalHeight;
  }

  private void drawContent(
      Graphics2D pCanvas, int pWidth, int pHeight, GraphStructure pGraphStructure) {
    pCanvas.setColor(COLOR_BACKGROUND);
    pCanvas.fillRect(0, 0, pWidth, pHeight);

    final int middle = pWidth / 2;
    int yPos = yPadding;
    for (GraphLevel level : pGraphStructure) {
      final int stateNum = level.getWidth();
      final int lineWidth = stateNum * scaling;

      final int xPos = middle - lineWidth / 2;

      if (strongHighlight) {
        Color levelBackground = level.getBackgroundColor();
        pCanvas.setColor(levelBackground);
        pCanvas.fillRect(0, yPos, pWidth, scaling);
      }

      pCanvas.setColor(COLOR_NODE);
      pCanvas.fillRect(xPos, yPos, lineWidth, scaling);

      final int currentYPos = yPos;
      for (Pair<ImmutableIntArray, Color> p : level.getGroups()) {
        pCanvas.setColor(p.getSecondNotNull());
        p.getFirstNotNull()
            .forEach(
                idx -> pCanvas.fillRect(xPos + (idx - 1) * scaling, currentYPos, scaling, scaling));
      }

      yPos += scaling;
    }
  }

  public void write(Node pRoot, Path pOutputFile)
      throws IOException, InvalidConfigurationException {
    GraphStructure structure = getStructure(pRoot);

    int finalWidth = getWidth(structure);
    int finalHeight = getHeight(structure);

    Graphics2D g = canvasHandler.createCanvas(finalWidth, finalHeight);
    drawContent(g, finalWidth, finalHeight, structure);

    Path fullOutputFile = Path.of(pOutputFile + "." + imageFormat);
    canvasHandler.writeToFile(fullOutputFile);
  }

  private interface CanvasProvider {
    Graphics2D createCanvas(int pWidth, int pHeight);

    void writeToFile(Path pOutputFile) throws IOException, InvalidConfigurationException;
  }

  private static class BitmapProvider implements CanvasProvider {

    private String imageFormat;
    private BufferedImage bufferedImage = null;

    BitmapProvider(String pFormat) {
      imageFormat = pFormat;
    }

    @Override
    public Graphics2D createCanvas(int pWidth, int pHeight) {
      checkState(
          bufferedImage == null,
          "createCanvas can only be called after writing the old " + "canvas to a file");
      bufferedImage = new BufferedImage(pWidth, pHeight, BufferedImage.TYPE_3BYTE_BGR);
      return bufferedImage.createGraphics();
    }

    @Override
    public void writeToFile(Path pOutputFile) throws IOException, InvalidConfigurationException {
      checkState(bufferedImage != null, "Canvas not created");
      try (FileImageOutputStream out = new FileImageOutputStream(pOutputFile.toFile())) {
        boolean success = ImageIO.write(bufferedImage, imageFormat, out);
        if (!success) {
          throw new InvalidConfigurationException(
              "ImageIO can't handle given format: " + imageFormat);
        }
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
      // This object creation takes a long time, so only do this on demand!
      // It takes a long time because of the call to FontManagerFactory.getInstance()
      // in Font#getFont2D() . It is called by org.apache.batik.svggen.DOMTreeManager
      // and currently there is no way to tell batik that we don't even need fonts.
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
