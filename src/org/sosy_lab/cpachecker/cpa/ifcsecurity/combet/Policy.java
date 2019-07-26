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
 *         &lt;element ref="{}description" minOccurs="0"/>
 *         &lt;element ref="{}securityclasses" minOccurs="0"/>
 *         &lt;element ref="{}allowed" minOccurs="0"/>
 *         &lt;element ref="{}notallowed" minOccurs="0"/>
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
@XmlRootElement(name = "policy")
public class Policy {

    protected String description;
    protected Securityclasses securityclasses;
    protected Allowed allowed;
    protected Notallowed notallowed;

    /**
     * Ruft den Wert der description-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Legt den Wert der description-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Ruft den Wert der securityclasses-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Securityclasses }
     *     
     */
    public Securityclasses getSecurityclasses() {
        return securityclasses;
    }

    /**
     * Legt den Wert der securityclasses-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Securityclasses }
     *     
     */
    public void setSecurityclasses(Securityclasses value) {
        this.securityclasses = value;
    }

    /**
     * Ruft den Wert der allowed-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Allowed }
     *     
     */
    public Allowed getAllowed() {
        return allowed;
    }

    /**
     * Legt den Wert der allowed-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Allowed }
     *     
     */
    public void setAllowed(Allowed value) {
        this.allowed = value;
    }

    /**
     * Ruft den Wert der notallowed-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Notallowed }
     *     
     */
    public Notallowed getNotallowed() {
        return notallowed;
    }

    /**
     * Legt den Wert der notallowed-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Notallowed }
     *     
     */
    public void setNotallowed(Notallowed value) {
        this.notallowed = value;
    }

}
