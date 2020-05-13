/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.collector;

import static com.google.common.collect.FluentIterable.from;
import static java.util.logging.Level.WARNING;

import com.google.common.base.Charsets;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix="cpa.collector")
public class CollectorStatistics implements Statistics {



  @Option(secure=true, name="export", description="export collector as .dot file")
  private boolean exportARG = true;

  @Option(secure=true, name="file",
      description="export collector as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argFile = Paths.get("collector.dot");


  private final CollectorCPA cpa;
  private final LogManager logger;
  //private static final String HTML_TEMPLATE = "collectorreport.html";
  private static final String HTML_TEMPLATE = "collectortable.html";
  private static final String CSS_TEMPLATE = "collectortable.css";
  private static final String JS_TEMPLATE = "collectortable.js";
  private Collection<ARGState> reachedcollectionARG = new ArrayList<ARGState>();
  private CollectorARGStateGenerator argStateGenerator;
  private CollectorARGStateGenerator test;
  private Collection<ARGState> reconstructedCollection;
  private LinkedHashMap<ARGState, Boolean> destroyedStates;
  private Boolean aftermerge;
  private LinkedHashMap<ARGState, Boolean> linkedmergepartner;
  private myARGState myARGState1;
  private myARGState myARGState2;
  private final LinkedHashMap<ARGState,ARGState> linkedparents = new LinkedHashMap<>();
  private LinkedHashMap<ARGState, Boolean> linkedDestroyer = new LinkedHashMap<>();
  private LinkedHashMap<ARGState, Boolean> linkedmergepartnerother = new LinkedHashMap<>();
  private ARGState newarg1;
  private ARGState newarg2;
  private ARGState newarg;
  private myARGState myARGStatetransfer;
  private ARGState convertedARGStatetransfer;
  private ARGState convertedparenttransfer;
  private ARGState newarg3;


  public CollectorStatistics(CollectorCPA ccpa, Configuration config,LogManager pLogger) throws InvalidConfigurationException {
    this.cpa = ccpa;
    this.logger=pLogger;

    config.inject(this, CollectorStatistics.class);
  }

  @Override
  public String getName() {
    return "CollectorCPA";
  } //return null wenn ich eh keine Statistiken will

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {

  //makeFile(result, reached);

    if (!reached.isEmpty() && reached.getFirstState() instanceof CollectorState) {
      //argStateGenerator = new CollectorARGStateGenerator(logger,reached);
      //reconstructedCollection = argStateGenerator.getCollection();
      //destroyedStates = argStateGenerator.getDestroyed();
      //linkedmergepartner = argStateGenerator.getLinkedmergepartner();
     // logger.log(Level.INFO, "sonja linkedmergepartner:\n" + linkedmergepartner);
      //logger.log(Level.INFO, "sonja destroyedStates:\n" + destroyedStates);
      //logger.log(Level.INFO, "sonja reconstructedCollection:\n" + reconstructedCollection);
      //makeFile2(reconstructedCollection);
      makeFile3(reached);

    }

    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put("Sonja", 42);//hier können statistics gedruckt werden, siehe andere Klassen
    writer.put("sonja result", result);
    writer.put("Sonja reached", reached.toString()) ;
    //writer.put("Sonja reconstructed", reconstructedCollection.toString());
    writer.put("Sonja reconstructed", reachedcollectionARG.toString());
    }

  private void makeFile(Result result, UnmodifiableReachedSet reached) {
      try{

        for (AbstractState rootState: reached) {
          CollectorState cstate = (CollectorState) rootState;
          ARGState argstate = cstate.getARGState();


          reachedcollectionARG.add(argstate);
          //logger.log(Level.INFO, "sonja got the Size: " + reachedcollectionARG.size());
          //ARGState first = getFirst(reachedcollectionARG);
          //logger.log(Level.INFO, "sonja got the first: " + first);
          //logger.log(Level.INFO, "sonja got the ARGState: " + argstate);
          //logger.log(Level.INFO, "sonja got them ALL: " + reachedcollectionARG);

          int i = 0;
          String filenamepart1 = "./output/etape_";
          String filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
          File file = new File(filenamefinal);
          while (file.exists()) {
            filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
            file = new File(filenamefinal);
            i++;
          }
          file.createNewFile();
          Writer writer = new FileWriter(file, false);
          BufferedWriter bw = new BufferedWriter(writer);

          //ARGToDotWriter.write(bw,reachedcollectionARG,"Test Sonja");
          ARGToDotWriter.write(bw,reconstructedCollection,"Test Reconstruction Sonja");

          bw.close();
        }


        BufferedReader reader =
            Resources.asCharSource(Resources.getResource(getClass(), HTML_TEMPLATE), Charsets.UTF_8)
                .openBufferedStream();
        Writer writerhtml = IO.openOutputFile(Paths.get("./output/SonjasFile.html"),
            Charsets.UTF_8);
        BufferedWriter bwhtml = new BufferedWriter(writerhtml);
        String line2;
        while (null != (line2 = reader.readLine())){
          logger.log(Level.INFO, "sonja will lesen " + line2);
          //bwhtml.write(line2);
          if (line2.contains("REPORT_CSS")) {
            //insertCss(writer);
            bwhtml.write("<style>" + "\n");
            Resources.asCharSource(Resources.getResource(getClass(), CSS_TEMPLATE), Charsets.UTF_8)
                .copyTo(bwhtml);
            bwhtml.write("</style>");
          } else if (line2.contains("REPORT_JS")){
            logger.log(Level.INFO, "sonja will javascript " );
            //insertJs(writer, cfa, dotBuilder, counterExample);
                     Resources.asCharSource(Resources.getResource(getClass(), JS_TEMPLATE), Charsets.UTF_8)
                         .copyTo(bwhtml);
          } else {
            bwhtml.write(line2 + "\n");
          }
        }
        bwhtml.close();

    }catch (IOException e) {
  logger.logUserException(
      WARNING, e, "Could not create Sonjas file.");
}
  }

  private void makeFile3(UnmodifiableReachedSet reached){
    //try{

    for (AbstractState entry : reached.asCollection()) {

      myARGStatetransfer = ((CollectorState) entry).getMyARGTransfer();

      if (myARGStatetransfer != null) {
        convertedARGStatetransfer = myARGStatetransfer.getARGState();
        convertedparenttransfer = myARGStatetransfer.getparentARGState();
        AbstractState wrappedmyARG = ((CollectorState) entry).getMyARGTransfer().getwrappedState();

        if (reachedcollectionARG.size() == 0) {
          newarg = new ARGState(wrappedmyARG, null);
          newarg.markExpanded();
          linkedparents.put(convertedARGStatetransfer, newarg);
        } else {

          if (linkedparents.containsKey(convertedparenttransfer)) {
            ARGState current = linkedparents.get(convertedparenttransfer);
            //logger.log(Level.INFO, "sonja current!!!!!! "+ "\n" + current);
            newarg = new ARGState(wrappedmyARG, current);
            newarg.markExpanded();
            linkedparents.put(convertedARGStatetransfer, newarg);
          } else {
            logger.log(Level.INFO, "sonja sollte hier nicht herkommen!!!!! ");
          }
        }
        //logger.log(Level.INFO, "sonja NEWARG "+ newarg.toString());
        reachedcollectionARG.add(newarg);
        makeFileonly(reachedcollectionARG);

      }

      Boolean merged = ((CollectorState) entry).ismerged();
      if (merged) {
        myARGState1 = ((CollectorState) entry).getTestmyARG();
        myARGState2 = ((CollectorState) entry).getTestmyARG2();
        if (myARGState1 != null && myARGState2 != null) {
          ARGState convertedARGState1 = myARGState1.getARGState();
          ARGState convertedparent1 = myARGState1.getparentARGState();
          //logger.log(Level.INFO, "sonja converted 1 !!!! " + "\n"+ convertedARGState1.toString());
          //logger.log(Level.INFO, "sonja converted_Parent 1!!!! "+ "\n"+ convertedparent1.toString());
          ARGState convertedARGState2 = myARGState2.getARGState();
          ARGState convertedparent2 = myARGState2.getparentARGState();
          //logger.log(Level.INFO, "sonja converted 2 !!!! " + "\n"+ convertedARGState2.toString());
          //logger.log(Level.INFO, "sonja converted_Parent 2!!!! "+ "\n"+ convertedparent2.toString());


          AbstractState wrappedmyARG1 = ((CollectorState) entry).getTestmyARG().getwrappedState();
          AbstractState wrappedmyARG2 = ((CollectorState) entry).getTestmyARG2().getwrappedState();

          ARGState mergedstate = ((CollectorState) entry).getARGState();
          //logger.log(Level.INFO, "sonja mergedstate "+ mergedstate.toString());
          if (linkedparents.containsKey(convertedparent1) && linkedparents
              .containsKey(convertedparent2)) {
            AbstractState c = mergedstate.getWrappedState();
            Object parent2 = linkedparents.get(convertedparent2);
            Object parent1 = linkedparents.get(convertedparent1);


           final ARGState current1 = linkedparents.get(convertedparent1);
           final ARGState current2 = linkedparents.get(convertedparent2);
//###################

            final Map<ARGState, ARGState> hashmaptest = Collections.unmodifiableMap(linkedparents);
            final ARGState currenthash1 = hashmaptest.get(convertedparent1);
            final List<ARGState> list = new ArrayList<>();
            list.add(0,current1);
            list.add(1,current2);
            final List<ARGState> unmodlist= Collections.unmodifiableList(new ArrayList<>(list));
            //ArrayList<ARGState> alist = (ArrayList<ARGState>) ((ArrayList<ARGState>) list).clone();
            ImmutableList<ARGState> immutableList =
                ImmutableList.copyOf(list);
//ARGState c1 = alist.get(0);
ARGState c11 = immutableList.get(0);
ARGState c111 = unmodlist.get(0);

//##################
            newarg1 = new ARGState(wrappedmyARG1, current1);
            reachedcollectionARG.add(newarg1);
            makeFileonly(reachedcollectionARG);
            newarg2 = new ARGState(wrappedmyARG2, current2);
            reachedcollectionARG.add(newarg2);
            makeFileonly(reachedcollectionARG);

            boolean destroyed1 = convertedARGState1.isDestroyed();
            boolean destroyed2 = convertedARGState2.isDestroyed();
            linkedparents.put(convertedARGState1, newarg1);
            linkedDestroyer.put(newarg1, destroyed1);
            linkedparents.put(convertedARGState2, newarg2);
            linkedDestroyer.put(newarg2, destroyed2);
            linkedmergepartnerother.put(newarg2, true);
            linkedmergepartnerother.put(newarg1, true);

            reachedcollectionARG.remove(newarg2);
            reachedcollectionARG.remove(newarg1);
            newarg1.removeFromARG();
            newarg2.removeFromARG();



            newarg3 = new ARGState(c, current2);
            newarg3.addParent(current1);
            linkedmergepartnerother.put(newarg3, false);
            linkedparents.put(mergedstate, newarg3);
            //logger.log(Level.INFO, "sonja NewARg with parents "+ newarg.toString());
            reachedcollectionARG.add(newarg3);
            makeFileonly(reachedcollectionARG);
          }
        }
      }


     /** if(reachedcollectionARG.size()!= 0) {

      int i = 0;
      String filenamepart1 = "./output/etape_";
      String filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
      File file = new File(filenamefinal);
      while (file.exists()) {
        filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
        file = new File(filenamefinal);
        i++;
      }
      file.createNewFile();
      Writer writer = new FileWriter(file, false);
      BufferedWriter bw = new BufferedWriter(writer);

        ARGToDotWriter.write(bw, reachedcollectionARG, "Test Reconstruction Sonja");

      bw.close();
      }
    }
    }catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create Sonjas file.");
    }**/

    logger.log(Level.INFO, "sonja reachedcollection "+ reachedcollectionARG.toString());
  }

  }
  private void makeFile2(Collection<ARGState> reached) {
    try{

      for (ARGState rootState: reached) {
        ARGState argstate = rootState;

        reachedcollectionARG.add(argstate);

        logger.log(Level.INFO, "sonja ROOTSTATES:\n" + argstate);



        int i = 0;
        String filenamepart1 = "./output/etape_";
        String filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
        File file = new File(filenamefinal);
        while (file.exists()) {
          filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
          file = new File(filenamefinal);
          i++;
        }
        file.createNewFile();
        Writer writer = new FileWriter(file, false);
        BufferedWriter bw = new BufferedWriter(writer);

        ARGToDotWriter.write(bw,reachedcollectionARG,"Test Reconstruction Sonja");
        /**ARGToDotWriter.write(
            bw, rootState, ARGState::getChildren, Predicates.alwaysTrue(), Objects::nonNull);**/

        bw.close();


        if (linkedmergepartner.containsKey(argstate)) {
          if (linkedmergepartner.get(argstate) == true) {
            logger.log(Level.INFO,
                "sonja das muss weg\n" + linkedmergepartner.get(argstate) + "\n" + argstate);
            reachedcollectionARG.remove(argstate);
            logger.log(Level.INFO,
                "sonja das muss weg Parents\n" + argstate.getParents());
            logger.log(Level.INFO,
                "sonja das muss weg children\n" + argstate.getChildren());

          } else {
            logger.log(Level.INFO,
                "sonja das bleibt\n" + linkedmergepartner.get(argstate) + "\n" + argstate);
          }
        }
        if(destroyedStates.containsKey(argstate)) {
          if (destroyedStates.get(argstate) == true) {
            logger.log(Level.INFO, "sonja destroyedStates CONTAINS true:" + destroyedStates.get(argstate)
                + "\n" + argstate);
            reachedcollectionARG.remove(argstate);
          } else {
            logger.log(Level.INFO,
                "sonja destroyedStates CONTAINS:" + destroyedStates.get(argstate) + "\n"
                    + argstate);
            //reachedcollectionARG.remove(argstate);
          }
        }
      }
    }catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create Sonjas file.");
    }
  }


  private void insertCss(BufferedWriter pWriter) {
  }

  public ARGState getFirst(Collection<ARGState> collection){
      return collection.iterator().next();
    }


  private void makeFileonly(Collection<ARGState> pReachedcollectionARG) {
    try{
      int i = 0;
      String filenamepart1 = "./output/etape_";
      String filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
      File file = new File(filenamefinal);
      while (file.exists()) {
        filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
        file = new File(filenamefinal);
        i++;
      }
      file.createNewFile();
      Writer writer = new FileWriter(file, false);
      BufferedWriter bw = new BufferedWriter(writer);

      ARGToDotWriter.write(bw, pReachedcollectionARG, "Test Reconstruction Sonja");

      bw.close();
    }catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create Sonjas file.");
    }
  }

}
