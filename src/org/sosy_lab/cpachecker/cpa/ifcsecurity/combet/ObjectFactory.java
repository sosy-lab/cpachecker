
package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.sosy_lab.cpachecker.cpa.ifcsecurity.combet package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Identifier_QNAME = new QName("", "identifier");
    private final static QName _HashMethod_QNAME = new QName("", "hashMethod");
    private final static QName _Securityclass_QNAME = new QName("", "securityclass");
    private final static QName _File_QNAME = new QName("", "file");
    private final static QName _Additional_QNAME = new QName("", "additional");
    private final static QName _Name_QNAME = new QName("", "name");
    private final static QName _Description_QNAME = new QName("", "description");
    private final static QName _Location_QNAME = new QName("", "location");
    private final static QName _Version_QNAME = new QName("", "version");
    private final static QName _Entity_QNAME = new QName("", "entity");
    private final static QName _HashValue_QNAME = new QName("", "hashValue");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.sosy_lab.cpachecker.cpa.ifcsecurity.combet
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Policyitem }
     * 
     */
    public Policyitem createPolicyitem() {
        return new Policyitem();
    }

    /**
     * Create an instance of {@link Inferences }
     * 
     */
    public Inferences createInferences() {
        return new Inferences();
    }

    /**
     * Create an instance of {@link Securityclasses }
     * 
     */
    public Securityclasses createSecurityclasses() {
        return new Securityclasses();
    }

    /**
     * Create an instance of {@link Notallowed }
     * 
     */
    public Notallowed createNotallowed() {
        return new Notallowed();
    }

    /**
     * Create an instance of {@link Allowed }
     * 
     */
    public Allowed createAllowed() {
        return new Allowed();
    }

    /**
     * Create an instance of {@link Mapitem }
     * 
     */
    public Mapitem createMapitem() {
        return new Mapitem();
    }

    /**
     * Create an instance of {@link AdditionalExtension }
     * 
     */
    public AdditionalExtension createAdditionalExtension() {
        return new AdditionalExtension();
    }

    /**
     * Create an instance of {@link Project }
     * 
     */
    public Project createProject() {
        return new Project();
    }

    /**
     * Create an instance of {@link Resources }
     * 
     */
    public Resources createResources() {
        return new Resources();
    }

    /**
     * Create an instance of {@link Resource }
     * 
     */
    public Resource createResource() {
        return new Resource();
    }

    /**
     * Create an instance of {@link Hashes }
     * 
     */
    public Hashes createHashes() {
        return new Hashes();
    }

    /**
     * Create an instance of {@link Hash }
     * 
     */
    public Hash createHash() {
        return new Hash();
    }

    /**
     * Create an instance of {@link Results }
     * 
     */
    public Results createResults() {
        return new Results();
    }

    /**
     * Create an instance of {@link ResultExtension }
     * 
     */
    public ResultExtension createResultExtension() {
        return new ResultExtension();
    }

    /**
     * Create an instance of {@link Case }
     * 
     */
    public Case createCase() {
        return new Case();
    }

    /**
     * Create an instance of {@link Info }
     * 
     */
    public Info createInfo() {
        return new Info();
    }

    /**
     * Create an instance of {@link Analysis }
     * 
     */
    public Analysis createAnalysis() {
        return new Analysis();
    }

    /**
     * Create an instance of {@link Attributes }
     * 
     */
    public Attributes createAttributes() {
        return new Attributes();
    }

    /**
     * Create an instance of {@link AttributeExtension }
     * 
     */
    public AttributeExtension createAttributeExtension() {
        return new AttributeExtension();
    }

    /**
     * Create an instance of {@link Tool }
     * 
     */
    public Tool createTool() {
        return new Tool();
    }

    /**
     * Create an instance of {@link ConfigurationExtension }
     * 
     */
    public ConfigurationExtension createConfigurationExtension() {
        return new ConfigurationExtension();
    }

    /**
     * Create an instance of {@link Policy }
     * 
     */
    public Policy createPolicy() {
        return new Policy();
    }

    /**
     * Create an instance of {@link Mapping }
     * 
     */
    public Mapping createMapping() {
        return new Mapping();
    }

    /**
     * Create an instance of {@link Violations }
     * 
     */
    public Violations createViolations() {
        return new Violations();
    }

    /**
     * Create an instance of {@link Violation }
     * 
     */
    public Violation createViolation() {
        return new Violation();
    }

    /**
     * Create an instance of {@link Violationelement }
     * 
     */
    public Violationelement createViolationelement() {
        return new Violationelement();
    }

    /**
     * Create an instance of {@link Certificates }
     * 
     */
    public Certificates createCertificates() {
        return new Certificates();
    }

    /**
     * Create an instance of {@link AdditionalType }
     * 
     */
    public AdditionalType createAdditionalType() {
        return new AdditionalType();
    }

    /**
     * Create an instance of {@link AttributeType }
     * 
     */
    public AttributeType createAttributeType() {
        return new AttributeType();
    }

    /**
     * Create an instance of {@link ConfigurationType }
     * 
     */
    public ConfigurationType createConfigurationType() {
        return new ConfigurationType();
    }

    /**
     * Create an instance of {@link ResultType }
     * 
     */
    public ResultType createResultType() {
        return new ResultType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "identifier")
    public JAXBElement<String> createIdentifier(String value) {
        return new JAXBElement<String>(_Identifier_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "hashMethod")
    public JAXBElement<String> createHashMethod(String value) {
        return new JAXBElement<String>(_HashMethod_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "securityclass")
    public JAXBElement<String> createSecurityclass(String value) {
        return new JAXBElement<String>(_Securityclass_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "file")
    public JAXBElement<String> createFile(String value) {
        return new JAXBElement<String>(_File_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AdditionalExtension }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "additional")
    public JAXBElement<AdditionalExtension> createAdditional(AdditionalExtension value) {
        return new JAXBElement<AdditionalExtension>(_Additional_QNAME, AdditionalExtension.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "name")
    public JAXBElement<String> createName(String value) {
        return new JAXBElement<String>(_Name_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "description")
    public JAXBElement<String> createDescription(String value) {
        return new JAXBElement<String>(_Description_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "location")
    public JAXBElement<String> createLocation(String value) {
        return new JAXBElement<String>(_Location_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "version")
    public JAXBElement<String> createVersion(String value) {
        return new JAXBElement<String>(_Version_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "entity")
    public JAXBElement<String> createEntity(String value) {
        return new JAXBElement<String>(_Entity_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "hashValue")
    public JAXBElement<String> createHashValue(String value) {
        return new JAXBElement<String>(_HashValue_QNAME, String.class, null, value);
    }

}
