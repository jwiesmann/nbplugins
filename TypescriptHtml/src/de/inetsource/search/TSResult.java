package de.inetsource.search;

public class TSResult {

    private String filterResult;
    private boolean isArray;

    public TSResult(String filterResult, boolean isArray) {
        this.filterResult = filterResult;
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

}
