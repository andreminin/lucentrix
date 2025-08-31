package org.lucentrix.metaframe.runtime.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.InputStream;
import java.io.OutputStream;

@Getter
@EqualsAndHashCode
@ToString
public abstract class PluginConfig<S> {
    @JsonMerge
    protected S settings;
    @JsonIgnore
    protected final ConfigEnv configEnv;

    public PluginConfig(InputStream inputStream, ConfigEnv configEnv) {
        this.configEnv = configEnv;
        this.settings = load(inputStream);
    }

    public PluginConfig(S settings, ConfigEnv configEnv) {
        this.configEnv = configEnv;
        this.settings = settings;
    }

    protected abstract String getName();

    protected abstract S load(InputStream is);

    public abstract void save(OutputStream os);

    //Factory method
    protected abstract S defaultSettings();

}