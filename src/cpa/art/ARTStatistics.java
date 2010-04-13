package cpa.art;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

import common.configuration.Configuration;
import common.configuration.Option;
import common.configuration.Options;

import cpa.common.LogManager;
import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Statistics;
import cpa.symbpredabsCPA.SymbPredAbsAbstractElement;
import exceptions.InvalidConfigurationException;

@Options
public class ARTStatistics implements Statistics {

  @Option(name="ART.export")
  private boolean exportART = true;
  
  @Option(name="output.path")
  private String outputDirectory = "test/output/";

  @Option(name="ART.file")
  private String artFile = "ART.dot";

  @Option(name="cpas.art.errorPath.export")
  private boolean exportErrorPath = true;
  
  @Option(name="cpas.art.errorPath.file")
  private String errorPathFile = "ErrorPath.txt";
  
  private final LogManager logger;
  
  public ARTStatistics(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    this.logger = logger;
  }  
  
  @Override
  public String getName() {
    return null; // return null because we do not print statistics
  }

  @Override
  public void printStatistics(PrintWriter pOut, Result pResult,
      ReachedElements pReached) {
    if (exportART) {
      dumpARTToDotFile(pReached);
    }
    
    if (exportErrorPath) {
      AbstractElement lastElement = pReached.getLastElement();
      if (lastElement != null && lastElement.isError() && (lastElement instanceof ARTElement)) {
        writeToFile(AbstractARTBasedRefiner.buildPath((ARTElement)lastElement).toString(),
                    errorPathFile);
      }
    }
  }

  private void writeToFile(String content, String filename) {
    File outfile = new File(outputDirectory, filename);
    PrintWriter out;
    try {
      out = new PrintWriter(outfile);
    } catch (FileNotFoundException e) {
      logger.log(Level.WARNING,
          "Could not write to file ", outfile, ", (", e.getMessage(), ")");
      return;
    }
    
    out.println(content);
    out.flush();
    out.close();
    if (out.checkError()) {
      logger.log(Level.WARNING, "Could not write to file ", outfile);
    }
  }
  
  private void dumpARTToDotFile(ReachedElements pReached) {
    ARTElement firstElement = (ARTElement)pReached.getFirstElement();

    Deque<ARTElement> worklist = new LinkedList<ARTElement>();
    Set<Integer> nodesList = new HashSet<Integer>();
    Set<ARTElement> processed = new HashSet<ARTElement>();
    StringBuffer sb = new StringBuffer();
    StringBuffer edges = new StringBuffer();
    
    sb.append("digraph ART {\n");
    sb.append("style=filled; color=lightgrey; \n");

    worklist.add(firstElement);

    while(worklist.size() != 0){
      ARTElement currentElement = worklist.removeLast();
      if(processed.contains(currentElement)){
        continue;
      }
      processed.add(currentElement);
      if(!nodesList.contains(currentElement.getElementId())){
        String color;
        if (currentElement.isCovered()) {
          color = "green";
        } else if (currentElement.isError()) {
          color = "red";
        } else {
          SymbPredAbsAbstractElement symbpredabselem = currentElement.retrieveWrappedElement(SymbPredAbsAbstractElement.class);
          if (symbpredabselem != null && symbpredabselem.isAbstractionNode()) {
            color = "blue";
          } else {
            color = "white";
          }
        }

        CFANode loc = currentElement.retrieveLocationElement().getLocationNode();
        String label = (loc==null ? 0 : loc.getNodeNumber()) + "000" + currentElement.getElementId();
        sb.append("node [shape = diamond, color = " + color + ", style = filled, label=" + label +"] " + currentElement.getElementId() + ";\n");
        
        nodesList.add(currentElement.getElementId());
      }
      
      for (ARTElement covered : currentElement.getCoveredByThis()) {
        edges.append(covered.getElementId());
        edges.append(" -> ");
        edges.append(currentElement.getElementId());
        edges.append(" [style = dashed, label = \"covered by\"];\n");
      }
      
      for(ARTElement child : currentElement.getChildren()){
        CFAEdge edge = currentElement.getEdgeToChild(child);
        edges.append(currentElement.getElementId());
        edges.append(" -> ");
        edges.append(child.getElementId());
        edges.append(" [label = \"");
        edges.append(edge != null ? edge.toString().replace('"', '\'') : "");
        edges.append("\"];\n");
        if(!worklist.contains(child)){
          worklist.add(child);
        }
      }
    }
    sb.append(edges);
    sb.append("}\n");

    writeToFile(sb.toString(), artFile);
  }
}
