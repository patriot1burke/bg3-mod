package org.bg3.forge.scanner;

public class Bg3Library {
    public LocalizationCollector localizationCollector = new LocalizationCollector();
    public StatsCollector.Library statsCollector = new StatsCollector.Library();
    public RootTemplateCollector rootTemplateCollector = new RootTemplateCollector();

    public LocalizationCollector getLocalizationCollector() {
        return localizationCollector;
    }
    public StatsCollector.Library getStatsCollector() {
        return statsCollector;
    }
    public RootTemplateCollector getRootTemplateCollector() {
        return rootTemplateCollector;
    }
}
