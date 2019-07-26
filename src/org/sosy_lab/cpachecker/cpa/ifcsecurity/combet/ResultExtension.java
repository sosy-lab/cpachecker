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
 * Java-Klasse for resultExtension complex type.
 *
 * <p>
 * Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType name="resultExtension">
 *   &lt;complexContent>
 *     &lt;extension base="{}resultType">
 *       &lt;all>
 *         &lt;element ref="{}violations" minOccurs="0"/>
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
@XmlType(name = "resultExtension", propOrder = {
    "violations",
    "certificates"
})
public class ResultExtension
    extends ResultType
{

    protected Violations violations;
    protected Certificates certificates;

    /**
     * Ruft den Wert der violations-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Violations }
     *
     */
    public Violations getViolations() {
        return violations;
    }

    /**
     * Legt den Wert der violations-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Violations }
     *
     */
    public void setViolations(Violations value) {
        this.violations = value;
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
