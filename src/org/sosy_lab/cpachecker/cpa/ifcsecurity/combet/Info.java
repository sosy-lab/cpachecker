//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// �nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren.
// Generiert: 2018.04.20 um 12:34:41 PM CEST
//


package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>
 * Java-Klasse für anonymous complex type.
 *
 * <p>
 * Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element ref="{}project"/>
 *         &lt;element ref="{}analysis" minOccurs="0"/>
 *         &lt;element ref="{}tool" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "info")
public class Info {

    @XmlElement(required = true)
    protected Project project;
    protected Analysis analysis;
    protected Tool tool;

    /**
     * Ruft den Wert der project-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Project }
     *
     */
    public Project getProject() {
        return project;
    }

    /**
     * Legt den Wert der project-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Project }
     *
     */
    public void setProject(Project value) {
        this.project = value;
    }

    /**
     * Ruft den Wert der analysis-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Analysis }
     *
     */
    public Analysis getAnalysis() {
        return analysis;
    }

    /**
     * Legt den Wert der analysis-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Analysis }
     *
     */
    public void setAnalysis(Analysis value) {
        this.analysis = value;
    }

    /**
     * Ruft den Wert der tool-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Tool }
     *
     */
    public Tool getTool() {
        return tool;
    }

    /**
     * Legt den Wert der tool-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Tool }
     *
     */
    public void setTool(Tool value) {
        this.tool = value;
    }

}
