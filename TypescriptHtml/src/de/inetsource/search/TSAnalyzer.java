package de.inetsource.search;

import de.inetsource.TSInterface;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;

public class TSAnalyzer {

    private final Map<String, TSInterface> allInterfaces;
    private final Map<String, TSInterface> dynamicInterfaces;
    private final Map<String, String> allController;
    private WatchDir tsWatchService;
    private final Project thisProject;
    private File projectDir;

    public TSAnalyzer() {
        allInterfaces = new HashMap<>();
        allController = new HashMap<>();
        dynamicInterfaces = new HashMap<>();
        thisProject = lookupProject();
        projectDir = findProjectDir(thisProject);
        findFilesAndCreateTSInstances(projectDir);
        startWatcher();
    }

    public void copyInterfaceAndAddToDynamic(Map<String, TSInterface> foundInterfaces, String originalName, String newName, TSInterface originalInterface) {
        TSInterface tsInterface = findInterface(originalName, foundInterfaces);
        if (tsInterface != null) {
            if (tsInterface.getParent() == null) {
                if (originalInterface == null) {
                    originalInterface = tsInterface;
                }
                TSInterface tsi = copyInterface(originalInterface, newName);
                tsInterface.getChildren().put(newName, tsi);
                System.out.println("added dynamic interface " + newName + " to parent: " + tsInterface.getName());
            } else {
                copyInterfaceAndAddToDynamic(foundInterfaces, tsInterface.getParent().getName(), newName, tsInterface);
            }
        }
    }

    private TSInterface copyInterface(TSInterface originalInterface, String newName) {
        TSInterface tsi = new TSInterface(originalInterface.isExtendsInterface(), newName);
        tsi.setExtendsInterfaceName(originalInterface.getExtendsInterfaceName());
        tsi.setObjectFunctions(originalInterface.getObjectFunctions());
        tsi.setObjectProperties(originalInterface.getObjectProperties());
        return tsi;
    }

    public String getController(String controllerName) {
        return allController.get(controllerName);
    }

    private void startWatcher() {
        try {
            tsWatchService = new WatchDir(projectDir.toPath(), true, this);
            Thread t = new Thread(tsWatchService);
            t.start();
        } catch (IOException ex) {
        }
    }

    /**
     * search for ng-repeats and create dynamic interfaces. Dynamic interface
     * means that it might be in a angular specific term. For example
     * ng-repeat="myVar in person" would create a "myVar" interface, that i can
     * be found/added to the completion result. It also copies everything from
     * 'person'
     *
     * @param foundSuggestions
     * @param htmlContent
     */
    private void findAndAddDynamicInterfaces(Map<String, TSInterface> foundSuggestions, String htmlContent) {

        Matcher ngRepeat = TSMatcher.getMatcher(SearchPattern.NG_REPEAT_FINISHED, htmlContent);
        while (ngRepeat.find()) {
            for (TSInterface tsi : foundSuggestions.values()) {
                for (String varUsed : tsi.getObjectProperties().keySet()) {
                    if (varUsed.equals(ngRepeat.group(3))) {
                        Matcher m = TSMatcher.find(SearchPattern.TYPE_IS_ARRAY, tsi.getObjectProperties().get(varUsed));
                        if (m != null) {
                            tsi = findInterface(varUsed, foundSuggestions);

                            if (tsi != null) {
                                System.out.println("trying to create dynamic interface from: " + m.group(1) + " to: " + ngRepeat.group(2));
                                copyInterfaceAndAddToDynamic(foundSuggestions, tsi.getName(), ngRepeat.group(2), null);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * creates a list of the interfaces used in the found controller.
     *
     * @param controllerName name of the controller to look for (all classes are
     * stored in allController)
     * @param htmlContent html file is needed to find variables like
     * "ng-repeat='per in persons'" .. we add per as new interface
     * @return
     */
    public Map<String, TSInterface> findInterfacesFromController(String controllerName, String htmlContent, boolean controllerAsSyntax) {
        dynamicInterfaces.clear();
        Map<String, TSInterface> foundSuggestions = new HashMap<>();
        String controllerContent = allController.get(controllerName);
        if (controllerContent != null) {
            if (controllerAsSyntax) {
                Matcher controllerVar = TSMatcher.find(SearchPattern.NG_CONTROLLER_AS_SYNTAX, htmlContent);
                if (controllerVar!=null) {
                    TSInterface tsi = new TSInterface(false, controllerVar.group(3));
                    
                }
            }
            
            
            Matcher interfacePatternMatcher = TSMatcher.getMatcher(SearchPattern.INTERFACE_NAME, controllerContent);
            while (interfacePatternMatcher.find()) {
                String foundInterfaceName = controllerContent.substring(interfacePatternMatcher.start() + 2, interfacePatternMatcher.end());
                TSInterface found = allInterfaces.get(foundInterfaceName);
                if (found != null) {
                    System.out.println("FOUND: " + found.getName() + " >>>" + found.getObjectFunctions() + " >>> " + found.getObjectProperties());
                    foundSuggestions.put(foundInterfaceName, found);
                    if (found.isExtendsInterface() && allInterfaces.containsKey(found.getExtendsInterfaceName())) {
                        foundSuggestions.put(found.getExtendsInterfaceName(), allInterfaces.get(found.getExtendsInterfaceName()));
                        TSInterface extenededInterface = allInterfaces.get(found.getExtendsInterfaceName());
                        System.out.println("FOUND Extension: " + found.getExtendsInterfaceName());
                        found.getObjectFunctions().putAll(extenededInterface.getObjectFunctions());
                        found.getObjectProperties().putAll(extenededInterface.getObjectProperties());
                    }
                    buildChildren(found, 0);
                }
            }
        }
        findAndAddDynamicInterfaces(foundSuggestions, htmlContent);
        return foundSuggestions;
    }

    /**
     * runs through all file in the current project ending with ".ts". Ignores
     * all in "node_modules"
     *
     * @param projectDir
     */
    public final void findFilesAndCreateTSInstances(File projectDir) {
        Collection<File> foundFiles2 = FileUtils.listFiles(projectDir, new SuffixFileFilter(".ts"),
                TrueFileFilter.INSTANCE);

        for (File ff : foundFiles2) {
            if (!ff.getAbsolutePath().contains("node_modules") && ff.exists()) {
                createInterfaceForSingleFile(ff);
            }
        }
    }

    public void createInterfaceForSingleFile(File ff) {
        try {
            Date startDate = new Date();
            System.out.println("creating interface for:" + ff.getAbsolutePath());
            String content = FileUtils.readFileToString(ff);
            Matcher isController = TSMatcher.find(SearchPattern.IS_CLASS, content);
            if (isController != null) {
                System.out.println("found class:" + isController.group(2));
                allController.put(isController.group(2), content);
            }
            allInterfaces.putAll(createInterfaceFromFileContent(content));
            System.out.println("done:" + (new Date().getTime() - startDate.getTime()) + " ms");

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private Map<String, TSInterface> createInterfaceFromFileContent(String text) {

        long start = System.currentTimeMillis();
        Map<String, TSInterface> result = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            String line = reader.readLine();
            int bracketsCount = 0;
            boolean insideInterface = false;
            TSInterface tsi = null;
            while (line != null) {

                if (insideInterface) {
                    insideInterface = searchFunctionOrProperty(line, tsi, insideInterface, bracketsCount, result);
                }
                Matcher m = TSMatcher.find(SearchPattern.IS_INTERFACE, line);
                if (!insideInterface && m != null) {
                    boolean extendsInterface = line.contains("extends");
                    tsi = new TSInterface(extendsInterface, m.group(2));
                    if (extendsInterface) {
                        Matcher extendsInterfaceNameMatcher = TSMatcher.find(SearchPattern.DOES_EXTEND, line);
                        if (extendsInterfaceNameMatcher != null) {
                            String extendsInterfaceName = extendsInterfaceNameMatcher.group(2);
                            if (extendsInterfaceName.contains(".")) {
                                extendsInterfaceName = extendsInterfaceName.substring(extendsInterfaceName.lastIndexOf(".") + 1);
                            }
                            tsi.setExtendsInterfaceName(extendsInterfaceName);
                        }
                    }
                    insideInterface = true;
                }
                line = reader.readLine();
            }
        } catch (IOException exc) {
            // quit
        }
        System.out.printf(result.size() + " ==> in Reader: %d%n", System.currentTimeMillis() - start);
        return result;
    }

    public boolean searchFunctionOrProperty(String line, TSInterface tsi, boolean insideInterface, int bracketsCount, Map<String, TSInterface> result) {
        if (tsi != null) {
            Matcher simpleProp = TSMatcher.find(SearchPattern.SIMPLE_PROPERTY_OR_FUNCTION, line);
            if (simpleProp != null) {
                if (simpleProp.group(1).contains("(")) {
                    tsi.getObjectFunctions().put(simpleProp.group(1), simpleProp.group(2));
                } else {
                    tsi.getObjectProperties().put(simpleProp.group(1), simpleProp.group(2));
                }
            }
            insideInterface = isInterfaceDone(line, bracketsCount, insideInterface);
            if (!insideInterface) {
                result.put(tsi.getName(), tsi);
            }
        }
        return insideInterface;
    }

    private Project lookupProject() {
        Project p = TopComponent.getRegistry().getActivated().getLookup().lookup(Project.class);
        if (p == null) {
            DataObject dob = TopComponent.getRegistry().getActivated().getLookup().lookup(DataObject.class);
            if (dob != null) {
                FileObject fo = dob.getPrimaryFile();
                p = FileOwnerQuery.getOwner(fo);
                return p;
            }
        }
        return null;
    }

    private File findProjectDir(Project thisProject) {
        if (thisProject.getProjectDirectory().getParent() != null) {
            projectDir = FileUtil.toFile(thisProject.getProjectDirectory().getParent());
        } else {
            projectDir = FileUtil.toFile(thisProject.getProjectDirectory());
        }
        if (projectDir != null && projectDir.isDirectory()) {
            return projectDir;
        }
        return null;
    }

    public boolean isInterfaceDone(String line, int bracketsCount, boolean insideInterface) {
        if (line.contains("{")) {
            bracketsCount += StringUtils.countMatches(line, "{");
        }
        if (line.contains("}") && bracketsCount == 0) {
            insideInterface = false;
        } else if (line.contains("}")) {
            bracketsCount -= StringUtils.countMatches(line, "}");
        }
        return insideInterface;
    }

    public Map<String, TSInterface> getDynamicInterfaces() {
        return dynamicInterfaces;
    }

    private void buildChildren(TSInterface parentInterface, int maxLevel) {
        if (maxLevel <= 5) {
            for (String key : parentInterface.getObjectProperties().keySet()) {
                String interfaceName = parentInterface.getObjectProperties().get(key);

                Matcher m = TSMatcher.find(SearchPattern.TYPE_IS_ARRAY, interfaceName);
                boolean isArray = false;
                if (m != null) {
                    // strip array
                    interfaceName = m.group(1);
                    isArray = true;
                }

                if (allInterfaces.containsKey(interfaceName)) {
                    TSInterface globalInterface = allInterfaces.get(interfaceName);
                    System.out.println(maxLevel + " --> FOUND Child: " + parentInterface.getName() + " >> " + globalInterface.getName() + " added as " + key + " isArray:" + isArray);
                    TSInterface tsi = copyInterface(globalInterface, key);
                    tsi.setArray(isArray);
                    tsi.setParent(parentInterface);
                    parentInterface.getChildren().put(key, tsi);

                    buildChildren(tsi, maxLevel + 1);
                }
            }
        }
    }

    private TSInterface findInterface(String interfaceName, Map<String, TSInterface> foundSuggestions) {
        for (String tsiName : foundSuggestions.keySet()) {
            if (interfaceName.equals(tsiName)) {
                System.out.println(">>> FOUND IT!!!");
                return foundSuggestions.get(tsiName);
            } else {
                System.out.println(interfaceName + " != " + tsiName);
                if (!foundSuggestions.get(tsiName).getChildren().isEmpty()) {
                    return findInterface(interfaceName, foundSuggestions.get(tsiName).getChildren());
                }
            }
        }
        return null;
    }

}
