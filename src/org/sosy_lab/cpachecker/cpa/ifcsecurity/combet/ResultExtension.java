package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
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

    public Violations getViolations() {
        return violations;
    }

    public void setViolations(Violations value) {
        this.violations = value;
    }

    public Certificates getCertificates() {
        return certificates;
    }

    public void setCertificates(Certificates value) {
        this.certificates = value;
    }

}
