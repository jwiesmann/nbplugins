package de.inetsource.search;

public class TSResult {

    private String filterResult;
    private String variableUsed;
    private String realInterfaceName;
    private boolean isArray;

    public TSResult(String filterResult, String variableUsed, boolean isArray) {
        this.filterResult = filterResult;
        this.variableUsed = variableUsed;        
        this.isArray = isArray;
    }

    public String getFilterResult() {
        return filterResult;
    }

    public void setFilterResult(String filterResult) {
        this.filterResult = filterResult;
    }

    public boolean isIsArray() {
        return isArray;
    }

    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }

    public String getVariableUsed() {
        return variableUsed;
    }

    public void setVariableUsed(String variableUsed) {
        this.variableUsed = variableUsed;
    }

    public String getRealInterfaceName() {
        return realInterfaceName;
    }

    public void setRealInterfaceName(String realInterfaceName) {
        this.realInterfaceName = realInterfaceName;
    }

}
