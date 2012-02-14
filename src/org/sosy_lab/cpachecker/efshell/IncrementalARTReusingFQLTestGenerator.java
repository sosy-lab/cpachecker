package org.sosy_lab.cpachecker.efshell;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterTransferRelation;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.AddrMemoryCell;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.DataMemoryCell;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.FuncMemoryCell;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryBlock;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.PersMemory;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Scope;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fshell.cfa.Wrapper;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;
public class IncrementalARTReusingFQLTestGenerator implements FQLTestGenerator {

  private final Configuration mConfiguration;
  private final LogManager mLogManager;
  private final Wrapper mWrapper;
  private final LocationCPA mLocationCPA;
  private final CallstackCPA mCallStackCPA;

  private Map<String, CFAFunctionDefinitionNode> lCFAMap;

  public IncrementalARTReusingFQLTestGenerator(String pSourceFileName, String pEntryFunction) {

    CFAFunctionDefinitionNode lMainFunction;

    try {
      mConfiguration = FShell3.createConfiguration(pSourceFileName, pEntryFunction);
      mLogManager = new LogManager(mConfiguration);

      lCFAMap = FShell3.getCFAMap(pSourceFileName, mConfiguration, mLogManager);
      lMainFunction = lCFAMap.get(pEntryFunction);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    mWrapper = new Wrapper((FunctionDefinitionNode)lMainFunction, lCFAMap, mLogManager);

    try {
      mWrapper.toDot("test/output/wrapper.dot");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    /*
     * Initialize shared CPAs.
     */
    // location CPA
    try {
      mLocationCPA = (LocationCPA)LocationCPA.factory().createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }

    // callstack CPA
    CPAFactory lCallStackCPAFactory = CallstackCPA.factory();
    try {
      mCallStackCPA = (CallstackCPA)lCallStackCPAFactory.createInstance();
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }
  }


  @SuppressWarnings("unused")
  @Override
  public FShell3Result run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic, boolean pAlternating,TestCase pTestCase, PrintWriter out) {

    /*ProcMem pm = new ProcMem();
    Sigar h = new Sigar();
    try {
      pm = h.getProcMem(h.getPid());
    } catch (SigarException e) {
      //TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println("Resident" +pm.getResident()/1024/1024);
    System.out.println("Size: " +pm.getSize()/1024/1024);
    System.out.println("Share: " +pm.getShare()/1024/1024);
    System.out.println();*/


    Timer k = new Timer();
    k.start();
    long time;
    // new interpreter cpa
      InterpreterCPA cpa= new InterpreterCPA(pTestCase.getInputs(),lCFAMap);

      try {
        run2(cpa);
      } catch (Exception e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

    time = k.stop();



   int x =0,y=0;
   long t=0;



    if(Main.OINTPR==0){
      k.start();

      // old interpreter cpa

      try {
        run2(new org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA(pTestCase.getInputs()));
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      time = k.stop();


      //out.print(", " + InterpreterTransferRelation.TRCOUNT);

     // out.println(", " + org.sosy_lab.cpachecker.cpa.interpreter.InterpreterTransferRelation.TRCOUNT);


    }

//Ausgabe



    if (Main.CMPLXA==1){
      for(x=0;x<InterpreterTransferRelation.TRLIST.size();x++){
         out.print(InterpreterTransferRelation.TRLIST.get(x)+","+InterpreterTransferRelation.TRLISTTIME.get(x));
         out.print(","+InterpreterTransferRelation.PMSlist.get(x)+ ","+InterpreterTransferRelation.SVlist.get(x));
         out.print(","+ InterpreterTransferRelation.MBClist.get(x)+ ","+InterpreterTransferRelation.AMClist.get(x));
         out.println(","+ InterpreterTransferRelation.DMClist.get(x)+ ","+InterpreterTransferRelation.FMClist.get(x));

        t+=InterpreterTransferRelation.TRLISTTIME.get(x).longValue();
        if(InterpreterTransferRelation.TRLISTTIME.get(x).longValue()==0){
          y++;
        }

     }
        out.println("\n"+t+"\t"+y);
        out.println(InterpreterElement.count);
    }else if(Main.CMPLXA==2){

      for(Integer value  : PersMemory.PMdpt ){
        out.println(value);
      }
      out.println("---");
      for(Integer value  : Scope.Sdpt ){
        out.println(value);
      }
      out.println("---");
      for(Integer value  : MemoryBlock.Mdpt ){
        out.println(value);
      }
      out.println("---");
      for(Integer value  : AddrMemoryCell.Adpt ){
        out.println(value);
      }
      out.println("---");
      for(Integer value  : DataMemoryCell.Ddpt ){
        out.println(value);
      }
      out.println("---");
      for(Integer value  : FuncMemoryCell.Fdpt ){
        out.println(value);
      }






      }else{
        out.print(Long.toString(time));
        out.println("," +Long.toString(time));
      }

    out.close();


    return null;
  }

  public void run2(ConfigurableProgramAnalysis pInterpreterCPA) throws Exception {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();
    lComponentAnalyses.add(mLocationCPA);

    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);

    // explicit CPA
    lComponentAnalyses.add(pInterpreterCPA);


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(mConfiguration);
    lCPAFactory.setLogger(mLogManager);
    ConfigurableProgramAnalysis lCPA;
    lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager);

    AbstractElement lInitialElement = lCPA.getInitialElement(mWrapper.getEntry());
    Precision lInitialPrecision = lCPA.getInitialPrecision(mWrapper.getEntry());

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.TOPSORT);
    lReachedSet.add(lInitialElement, lInitialPrecision);

    lAlgorithm.run(lReachedSet);
  }
}

