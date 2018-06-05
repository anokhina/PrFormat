package ru.org.sevn.netbeans.format;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import net.revelc.code.formatter.java.JavaFormatter;
import net.revelc.code.formatter.html.HTMLFormatter;
import net.revelc.code.formatter.javascript.JavascriptFormatter;
import net.revelc.code.formatter.xml.XMLFormatter;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.Lookup;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataShadow;
import org.netbeans.api.editor.EditorRegistry;
import org.eclipse.jdt.core.JavaCore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Properties;
import javax.swing.text.Caret;
import net.revelc.code.formatter.model.ConfigReadException;
import net.revelc.code.formatter.model.ConfigReader;
import org.xml.sax.SAXException;
import net.revelc.code.formatter.ConfigurationSource;
import net.revelc.code.formatter.Formatter;
import net.revelc.code.formatter.LineEnding;
import org.apache.maven.plugin.logging.Log;
import org.openide.util.Exceptions;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

//https://github.com/born2snipe/compare-to-clipboard-nbm
//https://github.com/markiewb/show-path-in-title-netbeans-module/blob/master/src/de/markiewb/netbeans/plugin/showpathintitle/PathUtil.java#L162
//http://wiki.netbeans.org/YourFirstNetbeansModule
//https://platform.netbeans.org/tutorials/nbm-projectsamples.html
@ActionID(
        category = "Edit",
        id = "ru.org.sevn.netbeans.format.PrFormatter"
)
@ActionRegistration(
        iconBase = "ru/org/sevn/netbeans/format/Edit.png",
        displayName = "#CTL_PrFormatter"
)
@ActionReferences({
    @ActionReference(path = "Menu/Edit", position = 1325)
    ,
  @ActionReference(path = "Toolbars/Clipboard", position = 400)
    ,
  @ActionReference(path = "Shortcuts", name = "OS-F")
})
@Messages("CTL_PrFormatter=PrFormatter")
public final class PrFormatter implements ActionListener, ConfigurationSource {

    private static final Config CFG = new Config()
            .setCssFileConfig("formatter-maven-plugin/ph-css/css.properties")
            .setHtmlFileConfig("formatter-maven-plugin/jsoup/html.properties")
            .setJavaFileConfig("formatter-maven-plugin/eclipse/java.xml")
            .setJsFileConfig("formatter-maven-plugin/eclipse/javascript.xml")
            .setJsonFileConfig("formatter-maven-plugin/jackson/json.properties")
            .setXmlFileConfig("formatter-maven-plugin/jsoup/xml.properties")
            ;
    
    private static final Config CFG_SAMPLE = new Config()
            .setCssFileConfig("formatters/css.properties")
            .setHtmlFileConfig("formatters/html.properties")
            .setJavaFileConfig("formatters/java.xml")
            .setJsFileConfig("formatters/javascript.xml")
            .setJsonFileConfig("formatters/json.properties")
            .setXmlFileConfig("formatters/xml.properties")
            ;
    
    private JavaFormatter javaFormatter;
    private FakeCssFormatter cssFormatter;
    private HTMLFormatter htmlFormatter;
    private JavascriptFormatter javascriptFormatter;
    private FakeJsonFormatter jsonFormatter;
    private XMLFormatter xmlFormatter;
    
    private JavaFormatter javaFormatterDefault;
    private FakeCssFormatter cssFormatterDefault;
    private HTMLFormatter htmlFormatterDefault;
    private JavascriptFormatter javascriptFormatterDefault;
    private FakeJsonFormatter jsonFormatterDefault;
    private XMLFormatter xmlFormatterDefault;
    
    private long initTime = 0L;
    
    enum FileType {
        JAVA, CSS, HTML, JS, JSON, XML
    }
    
    public PrFormatter() {
        initDefaults();
        try {
            writeInit();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        try {
            init();
        } catch (Exception ex) {
            logErr(ex);
        }
    }
    
    private boolean needInit() {
        
        final File file = getConfigFileName();
        if (file.exists()) {
            if (isAfter(getConfigFileName(), initTime)) return true;
            if (config != null) {
                if (isAfter(getFileByName(config.getCssFileConfig()), initTime)) return true;
                if (isAfter(getFileByName(config.getHtmlFileConfig()), initTime)) return true;
                if (isAfter(getFileByName(config.getJavaFileConfig()), initTime)) return true;
                if (isAfter(getFileByName(config.getJsFileConfig()), initTime)) return true;
                if (isAfter(getFileByName(config.getJsonFileConfig()), initTime)) return true;
                if (isAfter(getFileByName(config.getXmlFileConfig()), initTime)) return true;
            }
        }
        return false;
    }
    
    private boolean isAfter(File file, long initTime) {
        if (file != null && file.exists()) {
            if (file.lastModified() > initTime) {
                return true;
            }
        }
        return false;
    }
    
    private void reinit() {
        if (needInit()) {
            try {
                init();
                inform("The formatter was initializes");
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
    private File getConfigDir() {
        String userHome = System.getProperty("user.home");
        File userDir = new File(userHome);
        File prFormatDir = new File(userDir, ".prformat");
        if (!prFormatDir.exists()) {
            prFormatDir.mkdirs();
        }
        return prFormatDir;
    }
    
    private void writeInit() throws IOException {
        final File prFormatDir = getConfigDir();
        File cfgSample = new File(prFormatDir, "sampleconfig.properties");
        if (!cfgSample.exists()) {
            try (FileOutputStream out = new FileOutputStream(cfgSample)) {
                CFG_SAMPLE.getProperties().store(out, "Sample configuration for PrFormatter Netbeans Plugin");
            }
        }
        createSampleAny(prFormatDir, CFG.getCssFileConfig(), CFG_SAMPLE.getCssFileConfig());
        createSampleAny(prFormatDir, CFG.getHtmlFileConfig(), CFG_SAMPLE.getHtmlFileConfig());
        createSampleAny(prFormatDir, CFG.getJavaFileConfig(), CFG_SAMPLE.getJavaFileConfig());
        createSampleAny(prFormatDir, CFG.getJsFileConfig(), CFG_SAMPLE.getJsFileConfig());
        createSampleAny(prFormatDir, CFG.getJsonFileConfig(), CFG_SAMPLE.getJsonFileConfig());
        createSampleAny(prFormatDir, CFG.getXmlFileConfig(), CFG_SAMPLE.getXmlFileConfig());
    }
        
    private void createSampleAny(final File outDir, final String resName, final String fileName) {
        try {
            createSample(outDir, resName, fileName);
        } catch (IOException ex) {
            logErr(ex);
        }
    }
    
    private void createSample(final File outDir, final String resName, final String fileName) throws IOException {
        final File file = new File(outDir, fileName);
        final File fileDir = file.getParentFile();
        if ( !fileDir.exists() ) {
            fileDir.mkdirs();
        }
        if (!file.exists()) {
            try (InputStream is = getResourceAsInputStream(resName)) {
                java.nio.file.Files.copy(is, file.toPath());
            }
        }
    }
    
    private void logErr(Throwable ex) {
        ex.printStackTrace();
        //Exceptions.printStackTrace(ex);
    }
    
    private Config config;
    
    private void init() throws Exception {
        initTime = 0L;
        final File file = getConfigFileName();
        if (file == null) {
            return;
        }
        initTime = new Date().getTime();
        try (final InputStream is = new FileInputStream(file)) {
            config = new Config(is);
            
            try {
                javaFormatter = createJavaFormatter(getOptionsFromConfigFile(config.getJavaFileConfig()));
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                javascriptFormatter = createJavascriptFormatter(getOptionsFromConfigFile(config.getJavaFileConfig()));
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                cssFormatter = createFormatter(new FakeCssFormatter(), config.getCssFileConfig());
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                htmlFormatter = createFormatter(new HTMLFormatter(), config.getHtmlFileConfig());
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                jsonFormatter = createFormatter(new FakeJsonFormatter(), config.getJsonFileConfig());
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                xmlFormatter = createFormatter(new XMLFormatter(), config.getXmlFileConfig());
            } catch (Exception ex) {
                logErr(ex);
            }
        }
    }
    
    private void initDefaults() {
            try {
                javaFormatterDefault = createJavaFormatter(getOptionsFromResource(CFG.getJavaFileConfig()));
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                javascriptFormatterDefault = createJavascriptFormatter(getOptionsFromResource(CFG.getJavaFileConfig()));
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                cssFormatterDefault = createFormatterRes(new FakeCssFormatter(), CFG.getCssFileConfig());
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                htmlFormatterDefault = createFormatterRes(new HTMLFormatter(), CFG.getHtmlFileConfig());
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                jsonFormatterDefault = createFormatterRes(new FakeJsonFormatter(), CFG.getJsonFileConfig());
            } catch (Exception ex) {
                logErr(ex);
            }
            try {
                xmlFormatterDefault = createFormatterRes(new XMLFormatter(), CFG.getXmlFileConfig());
            } catch (Exception ex) {
                logErr(ex);
            }
    }
    
    private PluginLog log = new PluginLog();
    
    private Lookup.Provider getEditor(TopComponent editor) {
        return editor;
    }
    
    private TopComponent getCurrentEditor() {
        WindowManager wm = WindowManager.getDefault();
        Mode editor = wm.findMode("editor");
        return editor.getSelectedTopComponent();
    }
    
    private FileObject getFileObjectWithShadowSupport(DataObject dataObject) {
        if (dataObject instanceof DataShadow) {
            DataShadow dataShadow = (DataShadow) dataObject;
            return dataShadow.getOriginal().getPrimaryFile();
        }
        return dataObject.getPrimaryFile();
    }
    
    private String getSelectedString() {
        return EditorRegistry.lastFocusedComponent().getSelectedText();        
    }
    
    private String selectAll() {
        EditorRegistry.lastFocusedComponent().selectAll();
        return EditorRegistry.lastFocusedComponent().getSelectedText();        
    }

    private void setSelectedString(final String str) {
        if (str != null) {
            EditorRegistry.lastFocusedComponent().replaceSelection(str);
        } else {
            //inform("Can't format selected text");
        }
    }
    
    private InputStream getResourceAsInputStream(final String newConfigFile) {
        return this.getClass().getClassLoader().getResourceAsStream(newConfigFile);
    }
    
    private Map<String, String> getOptionsFromResource(final String newConfigFile) throws IOException, SAXException, ConfigReadException {
        try (InputStream fis = getResourceAsInputStream(newConfigFile)) {
            return new ConfigReader().read(fis);
        }
    }
    
    private File getFileByName(final String newPropertiesFile) {
        final File file = new File(newPropertiesFile);
        if (!file.isAbsolute()) {
            return new File(getConfigDir(), newPropertiesFile);
        }
        return file;
    }
    
    private Map<String, String> getOptionsFromConfigFile(final String newConfigFile) throws IOException, SAXException, ConfigReadException {
        try (FileInputStream fis = new FileInputStream(getFileByName(newConfigFile))) {
            return new ConfigReader().read(fis);
        }
    }
    
    private Map<String, String> getEclipseDefaultOptions() {
        Map<String, String> options = new HashMap<>();
        options.put(JavaCore.COMPILER_SOURCE, getCompilerSources());
        options.put(JavaCore.COMPILER_COMPLIANCE, getCompilerCompliance());
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, getCompilerCodegenTargetPlatform());
        return options;
    }
    
    private Map<String, String> getOptionsFromPropertiesFile(String newPropertiesFile) throws IOException {
        final Map<String, String> map = new HashMap<>();
        if (newPropertiesFile != null) {
            try (FileInputStream fis = new FileInputStream(getFileByName(newPropertiesFile))) {
                properties2map(map, fis);
            }
        }
        return map;
    }    
    
    private Map<String, String> getOptionsFromPropertiesRes(String newPropertiesFile) throws IOException {
        final Map<String, String> map = new HashMap<>();
        if (newPropertiesFile != null) {
            try (InputStream fis = getResourceAsInputStream(newPropertiesFile)) {
                properties2map(map, fis);
            }
        }
        return map;
    }    
    
    private void properties2map(final Map<String, String> map, InputStream fis) throws IOException {
        Properties properties = new Properties();
        properties.load(fis);
        for (final String name : properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }
    }
    
    private JavaFormatter createJavaFormatter(final Map<String, String> options) {
        JavaFormatter javaFormatter = new JavaFormatter();
        javaFormatter.init(options, this);
        return javaFormatter;
    }
    private JavascriptFormatter createJavascriptFormatter(final Map<String, String> options) {
        JavascriptFormatter jsFormatter = new JavascriptFormatter();
        jsFormatter.init(options, this);
        return jsFormatter;
    }
    
    
    private <T extends Formatter> T createFormatter(final T formatter, final String configFile) throws Exception {
        formatter.init(getOptionsFromPropertiesFile(configFile), this);
        return formatter;
    }
    
    private <T extends Formatter> T createFormatterRes(final T formatter, final String configFile) throws Exception {
        formatter.init(getOptionsFromPropertiesRes(configFile), this);
        return formatter;
    }
    
    private File getConfigFileName() {
        return getConfigFileName("config.properties");
    }
    
    private File getConfigFileName(final String name) {
        final File prFormatDir = getConfigDir();
        File javaCfg = null;
        if (prFormatDir.exists() && prFormatDir.isDirectory()) {
            javaCfg = new File(prFormatDir, name);
            if (javaCfg.exists() && javaCfg.isFile() && javaCfg.canRead()) {
                return javaCfg;
            }
        }
        return null;        
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        reinit();

        Lookup.Provider provider = getEditor(getCurrentEditor());
        DataObject dataObject = provider.getLookup().lookup(DataObject.class);
        FileObject fileObject = provider.getLookup().lookup(FileObject.class);
        if (null != dataObject || null != fileObject) {
            final FileObject editedFile;
            if (null != dataObject) {
                editedFile = getFileObjectWithShadowSupport(dataObject);
            } else {
                editedFile = fileObject;
            }
            if (editedFile != null && editedFile.getPath() != null) {
                format(editedFile);
                return ;
            }
        }
        
        err("Can't find file name");
    }
    
    private void format(final FileObject editedFile) {
        final Rectangle visibleRect = EditorRegistry.lastFocusedComponent().getVisibleRect();
        final Caret caret = EditorRegistry.lastFocusedComponent().getCaret();
        String selectedStr = getSelectedString();
        if (selectedStr == null) {
            selectedStr = selectAll();
        }
        
        setSelectedString(getFormatted(editedFile, selectedStr));
        if (visibleRect != null) {
            EditorRegistry.lastFocusedComponent().scrollRectToVisible(visibleRect);
        }
    }
    
    private void inform(final String msg) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(msg, 
                        NotifyDescriptor.INFORMATION_MESSAGE));        
    }
    
    private void err(final String msg) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(msg, 
                        NotifyDescriptor.ERROR_MESSAGE));        
    }

    
    private String getFormatted(final FileObject editedFile, final String selectedString) {
        final String usage = ". Specify your settings in " + new File(getConfigDir(), "config.properties").getAbsolutePath();
        final LineEnding ending = LineEnding.LF; //TODO
        if (editedFile != null) {
            if (editedFile.getExt() != null) {
                switch(editedFile.getExt().toLowerCase()) {
                    case ".java":
                    case "java":
                    {
                        JavaFormatter formatter;
                        if (javaFormatter != null) {
                            formatter = javaFormatter;
                        } else if (javaFormatterDefault != null) {
                            formatter = javaFormatterDefault;
                            inform("Use default Java formatter" + usage);
                        } else {
                            formatter = null;
                            inform("Can't find any Java formatter");
                        }
                        if (formatter != null && selectedString != null) {
                            try {
                                return formatter.doFormat(selectedString, ending);
                            } catch (Exception ex) {
                                logErr(ex);
                                err("Format Java error:" + ex.getMessage());
                                return null;
                            }
                        }
                    }
                        break;
                    case ".js":
                    case "js":
                    {
                        JavascriptFormatter formatter;
                        if (javascriptFormatter != null) {
                            formatter = javascriptFormatter;
                        } else if (javascriptFormatterDefault != null) {
                            formatter = javascriptFormatterDefault;
                            inform("Use default JavaScript formatter" + usage);
                        } else {
                            formatter = null;
                            inform("Can't find any JavaScript formatter");
                        }
                        if (formatter != null && selectedString != null) {
                            try {
                                return formatter.doFormat(selectedString, ending);
                            } catch (Exception ex) {
                                logErr(ex);
                                err("Format JavaScript error:" + ex.getMessage());
                                return null;
                            }
                        }
                    }
                        break;
                    case ".css":
                    case "css":
                    {
                        FakeCssFormatter formatter;
                        if (cssFormatter != null) {
                            formatter = cssFormatter;
                        } else if (cssFormatterDefault != null) {
                            formatter = cssFormatterDefault;
                            inform("Use default CSS formatter" + usage);
                        } else {
                            formatter = null;
                            inform("Can't find any CSS formatter");
                        }
                        if (formatter != null && selectedString != null) {
                            try {
                                return formatter.doFormat(selectedString, ending);
                            } catch (Exception ex) {
                                logErr(ex);
                                err("Format CSS error:" + ex.getMessage());
                                return null;
                            }
                        }
                    }
                        break;
                    case ".html":
                    case ".htm":
                    case "html":
                    case "htm":
                    {
                        HTMLFormatter formatter;
                        if (htmlFormatter != null) {
                            formatter = htmlFormatter;
                        } else if (htmlFormatterDefault != null) {
                            formatter = htmlFormatterDefault;
                            inform("Use default HTML formatter" + usage);
                        } else {
                            formatter = null;
                            inform("Can't find any HTML formatter");
                        }
                        if (formatter != null && selectedString != null) {
                            try {
                                return formatter.doFormat(selectedString, ending);
                            } catch (Exception ex) {
                                logErr(ex);
                                err("Format HTML error:" + ex.getMessage());
                                return null;
                            }
                        }
                    }
                        break;
                    case ".xml":
                    case "xml":
                    {
                        XMLFormatter formatter;
                        if (xmlFormatter != null) {
                            formatter = xmlFormatter;
                        } else if (xmlFormatterDefault != null) {
                            formatter = xmlFormatterDefault;
                            inform("Use default XML formatter" + usage);
                        } else {
                            formatter = null;
                            inform("Can't find any XML formatter");
                        }
                        if (formatter != null && selectedString != null) {
                            try {
                                return formatter.doFormat(selectedString, ending);
                            } catch (Exception ex) {
                                logErr(ex);
                                err("Format XML error:" + ex.getMessage());
                                return null;
                            }
                        }
                    }
                        break;
                    case ".json":
                    case "json":
                    {
                        FakeJsonFormatter formatter;
                        if (jsonFormatter != null) {
                            formatter = jsonFormatter;
                        } else if (jsonFormatterDefault != null) {
                            formatter = jsonFormatterDefault;
                            inform("Use default JSON formatter" + usage);
                        } else {
                            formatter = null;
                            inform("Can't find any JSON formatter");
                        }
                        if (formatter != null && selectedString != null) {
                            try {
                                return formatter.doFormat(selectedString, ending);
                            } catch (Exception ex) {
                                logErr(ex);
                                err("Format JSON error:" + ex.getMessage());
                                return null;
                            }
                        }
                    }
                        break;
                }
            }
            err("Can't select formatter: unknown file type." + editedFile.getPath() + ":" + editedFile.getExt());
        }
        return null;
    }
    
    @Override
    public String getCompilerSources() {
        return "1.8";
    }

    @Override
    public String getCompilerCompliance() {
        return "1.8";
    }

    @Override
    public String getCompilerCodegenTargetPlatform() {
        return "1.8";
    }

    @Override
    public File getTargetDirectory() {
        return null;
    }

    @Override
    public Charset getEncoding() {
        return Charset.forName("UTF-8");
    }

    @Override
    public Log getLog() {
        return log;
    }
    
    
}
