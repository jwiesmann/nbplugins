package de.inetsource.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TSMatcher {

    public static Matcher find(SearchPattern pattern, String textToSearch) {
        Pattern p = Pattern.compile(pattern.getSearchPattern());
        Matcher m = p.matcher(textToSearch);
        if (m.find()) {
            return m;
        } else {
            return null;
        }
    }
    
    public static Matcher getMatcher(SearchPattern pattern, String textToSearch) {
        Pattern p = Pattern.compile(pattern.getSearchPattern());
        Matcher m = p.matcher(textToSearch);
        return m;
    }
}
