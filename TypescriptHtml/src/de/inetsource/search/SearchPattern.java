package de.inetsource.search;

/**
 * @author jwiesmann
 */
public enum SearchPattern {
    NG_CONTROLLER("(ng-controller=[\"'])([a-zA-Z]+)[\"']"),
    NG_CONTROLLER_AS_SYNTAX("(ng-controller=[\"'])([a-zA-Z]+) as ([a-zA-Z]+)[\"']"),
    NG_REPEAT("(ng-repeat=[\"'])([a-zA-Z]+) in ([a-zA-Z\\.]+)"),
    NG_MODEL("(ng-model=[\"'])([a-zA-Z\\.]{0,})"),
    NG_CLICK("(ng-click=[\"'])([a-zA-Z\\.]{0,})"),
    NG_REPEAT_FINISHED("(ng-repeat=[\"'])([a-zA-Z]+) in ([a-zA-Z\\.]+)[\"']"),
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
