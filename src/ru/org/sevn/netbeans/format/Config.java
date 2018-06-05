package ru.org.sevn.netbeans.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private String javaFileConfig;
    private String cssFileConfig;
    private String htmlFileConfig;
    private String jsFileConfig;
    private String jsonFileConfig;
    private String xmlFileConfig;
    
    public Config() {
        
    }
    
    public Config(final InputStream is) throws IOException {
        Properties prop = new Properties();
        prop.load(is);
        setJavaFileConfig(prop.getProperty("javaFileConfig"));
        setCssFileConfig(prop.getProperty("cssFileConfig"));
        setHtmlFileConfig(prop.getProperty("htmlFileConfig"));
        setJsFileConfig(prop.getProperty("jsFileConfig"));
        setJsonFileConfig(prop.getProperty("jsonFileConfig"));
        setXmlFileConfig(prop.getProperty("xmlFileConfig"));
    }

    public String getJavaFileConfig() {
        return javaFileConfig;
    }

    public Config setJavaFileConfig(String javaFileConfig) {
        this.javaFileConfig = javaFileConfig;
        return this;
    }

    public String getCssFileConfig() {
        return cssFileConfig;
    }

    public Config setCssFileConfig(String cssFileConfig) {
        this.cssFileConfig = cssFileConfig;
        return this;
    }

    public String getHtmlFileConfig() {
        return htmlFileConfig;
    }

    public Config setHtmlFileConfig(String htmlFileConfig) {
        this.htmlFileConfig = htmlFileConfig;
        return this;
    }

    public String getJsFileConfig() {
        return jsFileConfig;
    }

    public Config setJsFileConfig(String jsFileConfig) {
        this.jsFileConfig = jsFileConfig;
        return this;
    }

    public String getJsonFileConfig() {
        return jsonFileConfig;
    }

    public Config setJsonFileConfig(String jsonFileConfig) {
        this.jsonFileConfig = jsonFileConfig;
        return this;
    }

    public String getXmlFileConfig() {
        return xmlFileConfig;
    }

    public Config setXmlFileConfig(String xmlFileConfig) {
        this.xmlFileConfig = xmlFileConfig;
        return this;
    }
    
    public Properties getProperties() {
        final Properties ret = new Properties();
        ret.setProperty("javaFileConfig", javaFileConfig);
        ret.setProperty("cssFileConfig", cssFileConfig);
        ret.setProperty("htmlFileConfig", htmlFileConfig);
        ret.setProperty("jsFileConfig", jsFileConfig);
        ret.setProperty("jsonFileConfig", jsonFileConfig);
        ret.setProperty("xmlFileConfig", xmlFileConfig);
        return ret;
    }
}
