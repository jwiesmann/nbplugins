package de.inetsource.search;

public class TSResult {

    private String filterResult;
    private String variableUsed;
    private String realInterfaceName;
    private TSResultType type;

    public TSResult(String filterResult, String variableUsed, TSResultType type) {
        this.filterResult = filterResult;
        this.variableUsed = variableUsed;
        this.type = type;
    }

    public String getFilterResult() {
        return filterResult;
    }

    public void setFilterResult(String filterResult) {
        this.filterResult = filterResult;
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

    public TSResultType getType() {
        return type;
    }

}
