package org.sosy_lab.cpachecker.cpa.cer.io;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.cpa.cer.CERCPAStatistics;
import org.sosy_lab.cpachecker.cpa.cer.cex.Cex;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexFunctionHeadTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexFunctionReturnTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexNode;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexStatementTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexTransition;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.CounterexampleInformation;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.PrecisionInformation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Charsets;

public class CexExporterXML {

    private final CERCPAStatistics statistics;
    private final TimerWrapper exportTimer;
    private final DocumentBuilderFactory factory;
    private final TransformerFactory transformerFactory;
    private final StatCounter exportedCexCounter;
    private final boolean prettyString = true;

    public CexExporterXML(CERCPAStatistics pStatistics) {
        statistics = pStatistics;
        exportTimer = statistics.getFileExportTimer().getNewTimer();
        factory = DocumentBuilderFactory.newInstance();
        transformerFactory = TransformerFactory.newInstance();
        exportedCexCounter = statistics.getExportedCexCounter();
    }

    public void exportCexs(Collection<Cex> cexs, Path pPath) throws IOException {
        exportTimer.start();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element cexsElem = doc.createElement("Cexs");
            doc.appendChild(cexsElem);

            int cexCounter = 1;
            for (Cex cex : cexs) {
                Element cexElem = doc.createElement("Cex");
                cexElem.setAttribute("ID", String.valueOf(cexCounter));
                cexsElem.appendChild(cexElem);
                cexCounter++;

                Element cexNodesElem = doc.createElement("Nodes");
                cexElem.appendChild(cexNodesElem);
                Element cexTransitionsElem = doc.createElement("Transitions");
                cexElem.appendChild(cexTransitionsElem);

                CexNode currentCexNode = cex.getRootNode();
                while (true) {
                    Element cexNodeElem = doc.createElement("Node");
                    cexNodesElem.appendChild(cexNodeElem);
                    exportCexNode(doc, cexNodeElem, currentCexNode);

                    Optional<CexTransition> currentCexTransition =
                            currentCexNode.getLeavingTransition();
                    if (!currentCexTransition.isPresent()) {
                        break;
                    }

                    Element cexTransitionElem = doc.createElement("Transition");
                    cexTransitionsElem.appendChild(cexTransitionElem);
                    exportCexTransition(cexTransitionElem, currentCexTransition.get());

                    currentCexNode = currentCexTransition.get().getEndNode();
                }

                exportedCexCounter.inc();
            }

            transformAndWrite(doc, pPath);
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            exportTimer.stop();
        }
    }

    private static void exportCexNode(Document doc, Element cexNodeElem, CexNode currentNode) {
        cexNodeElem.setAttribute("ID", String.valueOf(currentNode.getId()));

        for (CounterexampleInformation cexInfo : currentNode.getCexInfos()) {
            if (cexInfo instanceof PrecisionInformation) {
                PrecisionInformation precisionInfo = (PrecisionInformation) cexInfo;
                Element valuePrecionElem = doc.createElement("ValuePrecision");
                cexNodeElem.appendChild(valuePrecionElem);
                for (MemoryLocation var : precisionInfo.getValuePrecison()) {
                    Element varElem = doc.createElement("Variable");
                    valuePrecionElem.appendChild(varElem);
                    varElem.setAttribute("QualifiedName", var.getExtendedQualifiedName());
                }
            }
        }
    }

    private static void exportCexTransition(Element cexTransitionElem, CexTransition pTransition) {
        if (pTransition instanceof CexStatementTransition) {
            CexStatementTransition transition = (CexStatementTransition) pTransition;
            cexTransitionElem.setAttribute("Type", CexStatementTransition.class.getSimpleName());
            cexTransitionElem.setAttribute("Statement", transition.getStatement());
        } else if (pTransition instanceof CexFunctionHeadTransition) {
            CexFunctionHeadTransition transition = (CexFunctionHeadTransition) pTransition;
            cexTransitionElem.setAttribute("Type", CexFunctionHeadTransition.class.getSimpleName());
            cexTransitionElem.setAttribute("Function", transition.getFunctionName());
        } else if (pTransition instanceof CexFunctionReturnTransition) {
            CexFunctionReturnTransition transition = (CexFunctionReturnTransition) pTransition;
            cexTransitionElem
                    .setAttribute("Type", CexFunctionReturnTransition.class.getSimpleName());
            cexTransitionElem.setAttribute("Function", transition.getFunctionName());
            cexTransitionElem.setAttribute("Statement", transition.getStatement());
        }

        cexTransitionElem.setAttribute("Start", String.valueOf(pTransition.getStartNode().getId()));
        cexTransitionElem.setAttribute("End", String.valueOf(pTransition.getEndNode().getId()));
    }

    private void transformAndWrite(Document doc, Path pPath)
            throws TransformerException, TransformerConfigurationException, IOException {
        Transformer transformer = transformerFactory.newTransformer();
        if (prettyString) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        }
        transformer.setOutputProperty(OutputKeys.ENCODING, Charsets.UTF_8.name());
        try (Writer w = IO.openOutputFile(pPath, Charsets.UTF_8)) {
            transformer.transform(new DOMSource(doc), new StreamResult(w));
        }
    }
}
