package de.inetsource.search;

/**
 * @author jwiesmann
 */
public enum SearchPattern {
    NG_CONTROLLER("(ng-controller=\")([a-zA-Z]+)\""),
    TYPE_IS_ARRAY("([\\S]+)(\\[[0-9]{0,}\\])"),
    INTERFACE_NAME(": [a-zA-Z\\.]+"),
    IS_INTERFACE("(interface )([\\S]+)"),
    IS_CLASS("(class )([\\S]+)"),
    DOES_EXTEND("(extends )([\\S]+)( \\{)"),
    SIMPLE_PROPERTY_OR_FUNCTION("(\\S+): ([a-zA-Z}\\[\\]]+);");
    
    private final String searchPattern;

    private SearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    public String getSearchPattern() {
        return searchPattern;
    }
    
    
    
    
}
