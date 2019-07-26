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
 *         &lt;element ref="{}securityclass"/>
 *         &lt;element ref="{}inferences"/>
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
@XmlRootElement(name = "policyitem")
public class Policyitem {

    @XmlElement(required = true)
    protected String securityclass;
    @XmlElement(required = true)
    protected Inferences inferences;

    /**
     * Ruft den Wert der securityclass-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSecurityclass() {
        return securityclass;
    }

    /**
     * Legt den Wert der securityclass-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSecurityclass(String value) {
        this.securityclass = value;
    }

    /**
     * Ruft den Wert der inferences-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Inferences }
     *
     */
    public Inferences getInferences() {
        return inferences;
    }

    /**
     * Legt den Wert der inferences-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Inferences }
     *
     */
    public void setInferences(Inferences value) {
        this.inferences = value;
    }

}
