//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.04.20 um 12:34:41 PM CEST 
//


package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für anonymous complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element ref="{}info" minOccurs="0"/>
 *         &lt;element ref="{}results" minOccurs="0"/>
 *         &lt;element ref="{}additional" minOccurs="0"/>
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
@XmlRootElement(name = "case")
public class Case {

    protected Info info;
    protected Results results;
    protected AdditionalExtension additional;

    /**
     * Ruft den Wert der info-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Info }
     *     
     */
    public Info getInfo() {
        return info;
    }

    /**
     * Legt den Wert der info-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Info }
     *     
     */
    public void setInfo(Info value) {
        this.info = value;
    }

    /**
     * Ruft den Wert der results-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Results }
     *     
     */
    public Results getResults() {
        return results;
    }

    /**
     * Legt den Wert der results-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Results }
     *     
     */
    public void setResults(Results value) {
        this.results = value;
    }

    /**
     * Ruft den Wert der additional-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link AdditionalExtension }
     *     
     */
    public AdditionalExtension getAdditional() {
        return additional;
    }

    /**
     * Legt den Wert der additional-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link AdditionalExtension }
     *     
     */
    public void setAdditional(AdditionalExtension value) {
        this.additional = value;
    }

}
