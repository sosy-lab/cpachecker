package org.sosy_lab.cpachecker.cpa.art;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix="cpas.art")
public class ARTCPA extends AbstractSingleWrapperCPA {

  private static class ARTCPAFactory extends AbstractSingleWrapperCPAFactory {

    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {      
      return new ARTCPA(getChild(), getConfiguration(), getLogger());
    }
  }
  
  public static CPAFactory factory() {
    return new ARTCPAFactory();
  }
  
  /**
   * Use join as default merge, because sep is only safe if all other cpas also use sep.
   */
  @Option(name="merge", toUppercase=true, values={"SEP", "JOIN"})
  private String mergeType = "JOIN";
  
  private final LogManager logger;
  
  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final Statistics stats;

  private ARTCPA(ConfigurableProgramAnalysis cpa, Configuration config, LogManager logger) throws InvalidConfigurationException {
    super(cpa);
    config.inject(this);
    
    this.logger = logger;
    abstractDomain = new ARTDomain(this);
    transferRelation = new ARTTransferRelation(cpa.getTransferRelation());
    precisionAdjustment = new ARTPrecisionAdjustment(cpa.getPrecisionAdjustment());
    if (mergeType.equals("SEP")){
      mergeOperator = MergeSepOperator.getInstance();
    } else if (mergeType.equals("JOIN")){
      mergeOperator = new ARTMergeJoin(getWrappedCpa());
    } else {
      throw new InternalError("Update list of allowed merge operators!");
    }
    stopOperator = new ARTStopSep(getWrappedCpa());  
    stats = new ARTStatistics(config, logger);
  }

  public AbstractDomain getAbstractDomain ()
  {
    return abstractDomain;
  }

  public TransferRelation getTransferRelation ()
  {
    return transferRelation;
  }

  public MergeOperator getMergeOperator ()
  {
    return mergeOperator;
  }

  public StopOperator getStopOperator ()
  {
    return stopOperator;
  }

  public PrecisionAdjustment getPrecisionAdjustment () {
    return precisionAdjustment;
  }
  
  @Override
  public AbstractElement getInitialElement (CFAFunctionDefinitionNode pNode) {
    // TODO some code relies on the fact that this method is called only one and the result is the root of the ART
    return new ARTElement(getWrappedCpa().getInitialElement(pNode), null);
  }

  protected LogManager getLogger() {
    return logger;
  }

  public ARTElement findHighest(ARTElement pLastElem, CFANode pLoc) throws CPAException {
    ARTElement tempRetVal = null;
    
    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();
    Set<ARTElement> handled = new HashSet<ARTElement>();

    workList.add(pLastElem);

    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (!handled.add(currentElement)) {
        // currentElement was already handled
        continue;
      }
      // TODO check - bottom element
      CFANode loc = currentElement.retrieveLocationElement().getLocationNode(); 
      if(loc == null) {
        assert false;
        continue;
      } else{
        if (loc.equals(pLoc)) {
          tempRetVal = currentElement;
        }
        workList.addAll(currentElement.getParents());
      }
    }

    if (tempRetVal == null) {
      throw new CPAException("Inconsistent ART, did not find element for " + pLoc);
    }
    return tempRetVal;

  }


  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }
}