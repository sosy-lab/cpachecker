package org.sosy_lab.cpachecker.cpa.cer.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.sosy_lab.cpachecker.cpa.cer.CERCPAStatistics;
import org.sosy_lab.cpachecker.cpa.cer.cex.Cex;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexFunctionHeadTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexFunctionReturnTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexState;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexStatementTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexTransition;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.CounterexampleInformation;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.PrecisionInformation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CexImporterXML {

    private final CERCPAStatistics statistics;
    private final TimerWrapper importTimer;
    private final DocumentBuilderFactory factory;
    private final StatCounter importedCexCounter;

    public CexImporterXML(CERCPAStatistics pStatistics) {
        statistics = pStatistics;
        importTimer = statistics.getFileImportTimer().getNewTimer();
        factory = DocumentBuilderFactory.newInstance();
        importedCexCounter = statistics.getImportedCexCounter();
    }

    public Collection<Cex> importCexs(Path pPath) throws IOException, NoSuchFileException {

        importTimer.start();
        try (InputStream inStream = Files.newInputStream(pPath)) {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inStream);

            Element cexsElem = doc.getDocumentElement();
            NodeList nList = cexsElem.getElementsByTagName("Cex");
            List<Cex> result = new ArrayList<>(nList.getLength());
            for (int i = 0; i < nList.getLength(); ++i) {
                Cex cex = importCex((Element) nList.item(i));
                if (cex != null) {
                    result.add(cex);
                }
                importedCexCounter.inc();
            }
            importTimer.stop();
            return result;
        } catch (NoSuchFileException e) {
            importTimer.stop();
            throw e;
        } catch (Exception e) {
            importTimer.stop();
            throw new IOException(e);
        }
    }

    private static Cex importCex(Element cexElem) throws InvalidInputException {
        Element transitionsElem = (Element) cexElem.getElementsByTagName("Transitions").item(0);
        Element nodesElem = (Element) cexElem.getElementsByTagName("Nodes").item(0);

        if (transitionsElem == null || nodesElem == null) {
            return null;
        }

        // call importCexNode before importTransitions to fill the cexNodeIdentifier
        Map<Integer, CexState> cexNodeIdentifier = new HashMap<>();
        NodeList nodeList = nodesElem.getElementsByTagName("Node");
        for (int i = 0; i < nodeList.getLength(); ++i) {
            importCexNode((Element) nodeList.item(i), cexNodeIdentifier);
        }
        NodeList transitionsList = transitionsElem.getElementsByTagName("Transition");
        List<CexTransition> transitions = new ArrayList<>(transitionsList.getLength());
        for (int i = 0; i < transitionsList.getLength(); ++i) {
            transitions
                    .add(importCexTransition((Element) transitionsList.item(i), cexNodeIdentifier));
        }

        if (transitions.size() <= 0) {
            return null;
        }

        CexState currentNode = transitions.get(0).getStartState();
        Cex cex = new Cex(currentNode);
        for (CexTransition transition : transitions) {
            currentNode.setLeavingTransition(transition);
            currentNode = transition.getEndState();
        }
        return cex;
    }

    private static CexState
            importCexNode(Element cexNodeElem, Map<Integer, CexState> cexNodeIdentifier) {
        int id = Integer.valueOf(cexNodeElem.getAttribute("ID"));
        CexState resultNode = new CexState();
        cexNodeIdentifier.put(id, resultNode);

        NodeList valuePrecNodes = cexNodeElem.getElementsByTagName("ValuePrecision");
        if (valuePrecNodes.getLength() == 1) {
            Element valuePrecElem =
                    (Element) cexNodeElem.getElementsByTagName("ValuePrecision").item(0);
            Set<CounterexampleInformation> cexInfos = new HashSet<>();

            NodeList varElem = valuePrecElem.getElementsByTagName("Variable");
            Set<MemoryLocation> valuePrecision = new HashSet<>(varElem.getLength());
            for (int i = 0; i < varElem.getLength(); ++i) {
                String varName =
                        varElem.item(i)
                                .getAttributes()
                                .getNamedItem("QualifiedName")
                                .getTextContent();
                valuePrecision.add(MemoryLocation.fromQualifiedName(varName));
                PrecisionInformation precInfo = new PrecisionInformation(valuePrecision);
                cexInfos.add(precInfo);
            }

            resultNode.setCexInfos(cexInfos);
        }

        return resultNode;
    }

    private static CexTransition
            importCexTransition(Element cexTransitionElem, Map<Integer, CexState> cexNodeIdentifier)
                    throws InvalidInputException {
        String type = cexTransitionElem.getAttribute("Type");
        int startNodeId = Integer.valueOf(cexTransitionElem.getAttribute("Start"));
        CexState startNode = cexNodeIdentifier.get(startNodeId);
        int endNodeId = Integer.valueOf(cexTransitionElem.getAttribute("End"));
        CexState endNode = cexNodeIdentifier.get(endNodeId);
        if (startNode == null || endNode == null) {
            throw new InvalidInputException("The nodes of this transition are unknown.");
        }

        if (type.equals(CexStatementTransition.class.getSimpleName())) {
            String statement = cexTransitionElem.getAttribute("Statement");
            return CexStatementTransition.create(startNode, statement, endNode);
        } else if (type.equals(CexFunctionHeadTransition.class.getSimpleName())) {
            String functionName = cexTransitionElem.getAttribute("Function");
            return CexFunctionHeadTransition.create(startNode, functionName, endNode);
        } else if (type.equals(CexFunctionReturnTransition.class.getSimpleName())) {
            String statement = cexTransitionElem.getAttribute("Statement");
            String functionName = cexTransitionElem.getAttribute("Function");
            return CexFunctionReturnTransition.create(startNode, statement, functionName, endNode);
        }

        throw new InvalidInputException("Unknown transition specification.");
    }
}
