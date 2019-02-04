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
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.collector.CollectorCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
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
  Collection<AbstractState> reachedcollection = new ArrayList<AbstractState>();
  Collection<ARGState> reachedcollectionARG = new ArrayList<ARGState>();

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

  makeFile(result, reached);

    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put("Sonja", 42);//hier können statistics gedruckt werden, siehe andere Klassen
    writer.put("sonja result", result);
    writer.put("Sonja reached", reached.toString()) ;
    }

    //richtig ist die Übergabe von unmodifiableReachedSet, Collection nur zu Testzwecken
  private void makeFile(Result result, UnmodifiableReachedSet reached) {
  //private void makeFile(Result result, Collection<AbstractState> reached) {
      try{

        for (AbstractState rootState: reached) {
          //logger.log(Level.INFO, "sonja got them: " + rootState);
          CollectorState cstate = (CollectorState) rootState;
          ARGState argstate = cstate.getARGState();


          reachedcollectionARG.add(argstate);
          logger.log(Level.INFO, "sonja got the Size: " + reachedcollectionARG.size());
          ARGState first = sillyExample(reachedcollectionARG);
          logger.log(Level.INFO, "sonja got the first: " + first);
          logger.log(Level.INFO, "sonja got the ARGState: " + argstate);
          logger.log(Level.INFO, "sonja got them ALL: " + reachedcollectionARG);


          int i = 0;
          String filenamepart1 = "./output/SonjasTestfiledaszweite";
          String filenamefinal = filenamepart1 + ".txt";
          File file = new File(filenamefinal);
          while (file.exists()) {
            i++;
            filenamefinal = filenamepart1 + Integer.toString(i) + ".txt";
            file = new File(filenamefinal);
          }
          file.createNewFile();
          Writer writer = new FileWriter(file, false);
          BufferedWriter bw = new BufferedWriter(writer);

          //???? schreibt vom ersten Knoten aufwärts alle ARGStates ins file..fortlaufend:
          //bw.write(reachedcollectionARG.toString());
          //??? dotWriter macht es aber rückwärts, bis zum Knoten mit der die collection gefüllt
          // ist WARUM?????
          //mit getParents auch nur "falschrum"
          ARGToDotWriter.write(
              bw,
              argstate,
              ARGState::getChildren,
              Predicates.alwaysTrue(),
              Predicates.alwaysFalse());
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

  private void insertCss(BufferedWriter pWriter) {
  }

  public ARGState sillyExample(Collection<ARGState> collection){
      return collection.iterator().next();
    }
}
