//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// �nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren.
// Generiert: 2018.04.20 um 12:34:41 PM CEST
//


package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>
 * Java-Klasse für attributeExtension complex type.
 *
 * <p>
 * Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType name="attributeExtension">
 *   &lt;complexContent>
 *     &lt;extension base="{}attributeType">
 *       &lt;all>
 *         &lt;element ref="{}policy" minOccurs="0"/>
 *         &lt;element ref="{}mapping" minOccurs="0"/>
 *         &lt;element ref="{}certificates" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeExtension", propOrder = {
    "policy",
    "mapping",
    "certificates"
})
public class AttributeExtension
    extends AttributeType
{

    protected Policy policy;
    protected Mapping mapping;
    protected Certificates certificates;

    /**
     * Ruft den Wert der policy-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Policy }
     *
     */
    public Policy getPolicy() {
        return policy;
    }

    /**
     * Legt den Wert der policy-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Policy }
     *
     */
    public void setPolicy(Policy value) {
        this.policy = value;
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

    /**
     * Ruft den Wert der certificates-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Certificates }
     *
     */
    public Certificates getCertificates() {
        return certificates;
    }

    /**
     * Legt den Wert der certificates-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Certificates }
     *
     */
    public void setCertificates(Certificates value) {
        this.certificates = value;
    }

}
