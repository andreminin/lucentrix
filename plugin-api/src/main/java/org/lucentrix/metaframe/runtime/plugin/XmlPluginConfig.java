package org.lucentrix.metaframe.runtime.plugin;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public abstract class XmlPluginConfig<S> extends PluginConfig<S> {

    protected XmlPluginConfig(InputStream is, ConfigEnv configEnv) {
        super(is, configEnv);
    }

    @Override
    protected S load(InputStream is) {
        try {
            File temp = File.createTempFile("cfg", ".xml");
            Files.copy(is, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Configurations configs = new Configurations();
            XMLConfiguration config = configs.xml(temp);
            return apply(config);
        } catch (Exception ex) {
            throw new RuntimeException("Error reading xml configuration", ex);
        }

    }

    protected abstract S apply(XMLConfiguration config);
}