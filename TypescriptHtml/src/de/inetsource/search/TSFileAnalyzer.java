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

public class TSFileAnalyzer {

    private final Map<String, TSInterface> allInterfaces;
    private final Map<String, String> allController;
    private WatchDir tsWatchService;
    private final Project thisProject;
    private File projectDir;

    public TSFileAnalyzer() {
        allInterfaces = new HashMap<>();
        allController = new HashMap<>();
        thisProject = lookupProject();
        projectDir = findProjectDir(thisProject);
        findFilesAndCreateTSInstances(projectDir);
        startWatcher();
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

    public Map<String, TSInterface> findInterfacesFromController(String controllerName) {
        Map<String, TSInterface> foundSuggestions = new HashMap<>();
        String controllerContent = allController.get(controllerName);
        if (controllerContent != null) {
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
                }
                
            }

            System.out.println("interfaces created:" + allInterfaces.size());
        }
        return foundSuggestions;
    }

    public final void findFilesAndCreateTSInstances(File projectDir) {
        Collection<File> foundFiles2 = FileUtils.listFiles(projectDir, new SuffixFileFilter(".ts"),
                TrueFileFilter.INSTANCE);

        for (File ff : foundFiles2) {
            if (!ff.getAbsolutePath().contains("node_modules")) {

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
                    Matcher simpleProp = TSMatcher.find(SearchPattern.SIMPLE_PROPERTY_OR_FUNCTION, line);
                    if (simpleProp != null && tsi != null) {
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

}
