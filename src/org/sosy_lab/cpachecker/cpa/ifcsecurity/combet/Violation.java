//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2018.04.20 um 12:34:41 PM CEST 
//


package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
 *       &lt;sequence>
 *         &lt;element ref="{}location"/>
 *         &lt;element ref="{}violationelement"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "location",
    "violationelement"
})
@XmlRootElement(name = "violation")
public class Violation {

    @XmlElement(required = true)
    protected String location;
    @XmlElement(required = true)
    protected Violationelement violationelement;

    /**
     * Ruft den Wert der location-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocation() {
        return location;
    }

    /**
     * Legt den Wert der location-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocation(String value) {
        this.location = value;
    }

    /**
     * Ruft den Wert der violationelement-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Violationelement }
     *     
     */
    public Violationelement getViolationelement() {
        return violationelement;
    }

    /**
     * Legt den Wert der violationelement-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Violationelement }
     *     
     */
    public void setViolationelement(Violationelement value) {
        this.violationelement = value;
    }

}
