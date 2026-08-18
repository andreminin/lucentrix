package org.lucentrix.ingest.runtime.plugin;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Getter
public class SinkContext extends PluginContext  {
 }
