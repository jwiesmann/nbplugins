package de.inetsource;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic class to identify interfaces
 *
 * @author jwiesmann
 */
public class TSInterface {

    private boolean extendsInterface;
    private boolean array;
    private Map<String, String> objectProperties;
    private Map<String, String> objectFunctions;
    private String name;
    private String extendsInterfaceName;
    private Map<String, TSInterface> children;
    private TSInterface parent;

    public TSInterface(boolean extendsInterface, String name) {
        this.extendsInterface = extendsInterface;
        this.name = name;
        this.objectFunctions = new HashMap<>();
        this.objectProperties = new HashMap<>();
        this.children = new HashMap<>();
    }

    public boolean isExtendsInterface() {
        return extendsInterface;
    }

    public void setExtendsInterface(boolean extendsInterface) {
        this.extendsInterface = extendsInterface;
    }

    public Map<String, String> getObjectProperties() {
        return objectProperties;
    }

    public void setObjectProperties(Map<String, String> objectProperties) {
        this.objectProperties = objectProperties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getObjectFunctions() {
        return objectFunctions;
    }

    public void setObjectFunctions(Map<String, String> objectFunctions) {
        this.objectFunctions = objectFunctions;
    }

    public String getExtendsInterfaceName() {
        return extendsInterfaceName;
    }

    public void setExtendsInterfaceName(String extendsInterfaceName) {
        this.extendsInterfaceName = extendsInterfaceName;
    }

    public Map<String, TSInterface> getChildren() {
        return children;
    }

    public void setChildren(Map<String, TSInterface> children) {
        this.children = children;
    }

    public boolean isArray() {
        return array;
    }

    public void setArray(boolean array) {
        this.array = array;
    }

    public TSInterface getParent() {
        return parent;
    }

    public void setParent(TSInterface parent) {
        this.parent = parent;
    }

}
