package org.lucentrix.metaframe.runtime.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@EqualsAndHashCode(callSuper = true, exclude = "mapper")
public abstract class JsonPluginConfig<S> extends PluginConfig<S> {
    private ObjectMapper mapper;


    public JsonPluginConfig(InputStream is, ConfigEnv configEnv) {
        super(is,configEnv);
    }

    public JsonPluginConfig(S config, ConfigEnv configEnv) {
        super(config, configEnv);
    }

    protected ObjectMapper createObjectMapper() {
        return new ObjectMapper();
    }

    protected abstract Class<S> getConfigBeanClass();

    @Override
    protected S load(InputStream is) {
        try {
            S settings = defaultSettings();

            if(mapper == null) {
                mapper = createObjectMapper();
            }

            return mapper.readerForUpdating(settings).readValue(is, getConfigBeanClass());
        } catch (IOException e) {
            throw new RuntimeException("Error loading json configuration", e);
        }
    }

    @Override
    public void save(OutputStream os) {
        try {
            if(mapper == null) {
                mapper = createObjectMapper();
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(os, this.settings);
        } catch (Exception ex) {
            throw new RuntimeException("Error saving json configuration", ex);
        }
    }
}