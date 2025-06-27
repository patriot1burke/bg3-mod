package org.bg3.forge.scanner;

public class Bg3Library {
    LocalizationCollector localizationCollector = new LocalizationCollector();
    StatsCollector.Library statsCollector = new StatsCollector.Library();
    RootTemplateCollector rootTemplateCollector = new RootTemplateCollector();

    public LocalizationCollector getLocalizationCollector() {
        return localizationCollector;
    }
    public StatsCollector.Library getStatsCollector() {
        return statsCollector;
    }
    public RootTemplateCollector getRootTemplateCollector() {
        return rootTemplateCollector;
    }

    public static class Equipment {
        public String name;
        public String description;
        public String mapKey;
        public String displayName;
    }

}
