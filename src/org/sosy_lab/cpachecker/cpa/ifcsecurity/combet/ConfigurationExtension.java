package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>
 * Java-Class for "configurationExtension"-complex type.
 *
 * <pre>
 * &lt;complexType name="configurationExtension">
 *   &lt;complexContent>
 *     &lt;extension base="{}configurationType">
 *       &lt;sequence>
 *         &lt;element ref="{}description" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "configurationExtension", propOrder = {
    "description"
})
public class ConfigurationExtension
    extends ConfigurationType
{

    protected String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

}
