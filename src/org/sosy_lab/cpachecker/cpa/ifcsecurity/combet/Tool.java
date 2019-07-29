package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java-Klasse for anonymous complex type.
 *
 * <p>
 * Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element ref="{}name"/>
 *         &lt;element ref="{}version" minOccurs="0"/>
 *         &lt;element name="configuration" type="{}configurationExtension" minOccurs="0"/>
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
@XmlRootElement(name = "tool")
public class Tool {

  @XmlElement(required = true)
  protected String name;
  protected String version;
  protected ConfigurationExtension configuration;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String value) {
    this.version = value;
  }

  public ConfigurationExtension getConfiguration() {
    return configuration;
  }

  public void setConfiguration(ConfigurationExtension value) {
    this.configuration = value;
  }

}
