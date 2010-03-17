package cpa.observeranalysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cfa.objectmodel.CFAEdge;
import cpa.common.LogManager;
import cpa.common.interfaces.AbstractElement;

public class ObserverExpressionArguments {
  private Map<String, ObserverVariable> observerVariables;
  private List<AbstractElement> abstractElements;
  private CFAEdge cfaEdge;
  private LogManager logger;

  ObserverExpressionArguments(Map<String, ObserverVariable> pObserverVariables,
      List<AbstractElement> pAbstractElements, CFAEdge pCfaEdge, LogManager pLogger) {
    super();
    if (pObserverVariables == null)
      observerVariables = Collections.emptyMap();
    else
      observerVariables = pObserverVariables;
    if (pAbstractElements == null)
      abstractElements = Collections.emptyList();
    else 
      abstractElements = pAbstractElements;
    cfaEdge = pCfaEdge;
    logger = pLogger;
  }

  void setObserverVariables(Map<String, ObserverVariable> pObserverVariables) {
    observerVariables = pObserverVariables;
  }

  Map<String, ObserverVariable> getObserverVariables() {
    return observerVariables;
  }

  List<AbstractElement> getAbstractElements() {
    return abstractElements;
  }

  CFAEdge getCfaEdge() {
    return cfaEdge;
  }
  
  LogManager getLogger() {
    return logger;
  }
}
