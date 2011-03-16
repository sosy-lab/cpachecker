package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeDomain;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress.product.ProgressProductAutomatonPrecisionAdjustment;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class ProductAutomatonCPA extends CompositeCPA {

  //public static ProductAutomatonCPA create(Collection<GuardedEdgeAutomatonCPA> pAutomatonCPAs) {
  public static ProductAutomatonCPA create(Collection<ConfigurableProgramAnalysis> pAutomatonCPAs, boolean pUseProgressPrecisionAdjustment) {
    Preconditions.checkNotNull(pAutomatonCPAs);
    Preconditions.checkArgument(pAutomatonCPAs.size() > 0);
    
    ImmutableList.Builder<AbstractDomain> domains = ImmutableList.builder();
    ImmutableList.Builder<TransferRelation> transferRelations = ImmutableList.builder();
    ImmutableList.Builder<StopOperator> stopOperators = ImmutableList.builder();
    ImmutableList.Builder<ConfigurableProgramAnalysis> lCPAs = ImmutableList.builder();

    for (ConfigurableProgramAnalysis sp : pAutomatonCPAs) {
      domains.add(sp.getAbstractDomain());
      transferRelations.add(sp.getTransferRelation());
      stopOperators.add(sp.getStopOperator());
      lCPAs.add(sp);
    }

    CompositeDomain compositeDomain = new CompositeDomain(domains.build());
    ProductAutomatonTransferRelation compositeTransfer = new ProductAutomatonTransferRelation(transferRelations.build());
    ProductAutomatonStopOperator compositeStop = new ProductAutomatonStopOperator(stopOperators.build());
    
    return new ProductAutomatonCPA(compositeDomain, compositeTransfer, compositeStop, lCPAs.build(), pUseProgressPrecisionAdjustment);
  }
  
  public ProductAutomatonCPA(AbstractDomain abstractDomain,
      TransferRelation transferRelation,
      StopOperator stopOperator,
      ImmutableList<ConfigurableProgramAnalysis> cpas, boolean pUseProgressPrecisionAdjustment) {
    super(abstractDomain, transferRelation, new MergeSepOperator(), stopOperator,
        pUseProgressPrecisionAdjustment?ProgressProductAutomatonPrecisionAdjustment.INSTANCE:ProductAutomatonPrecisionAdjustment.getInstance(), cpas);
  }

}
