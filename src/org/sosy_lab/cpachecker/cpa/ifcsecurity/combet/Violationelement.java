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
 *         &lt;element ref="{}mapitem"/>
 *         &lt;element ref="{}mapping"/>
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
    "mapitem",
    "mapping"
})
@XmlRootElement(name = "violationelement")
public class Violationelement {

    @XmlElement(required = true)
    protected Mapitem mapitem;
    @XmlElement(required = true)
    protected Mapping mapping;

    /**
     * Ruft den Wert der mapitem-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Mapitem }
     *     
     */
    public Mapitem getMapitem() {
        return mapitem;
    }

    /**
     * Legt den Wert der mapitem-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Mapitem }
     *     
     */
    public void setMapitem(Mapitem value) {
        this.mapitem = value;
    }

    /**
     * Ruft den Wert der mapping-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Mapping }
     *     
     */
    public Mapping getMapping() {
        return mapping;
    }

    /**
     * Legt den Wert der mapping-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Mapping }
     *     
     */
    public void setMapping(Mapping value) {
        this.mapping = value;
    }

}
