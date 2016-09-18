package com.geekdivers;

import java.util.Map;

/**
 *
 * @author geekdivers
 */
public class TSInterface {

    private boolean extendsInterface;
    private Map<String, String> objectProperties;
    private Map<String, String> objectFunctions;
    private String name;

    public TSInterface(boolean extendsInterface, String name) {
        this.extendsInterface = extendsInterface;
        this.name = name;
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
    

}
