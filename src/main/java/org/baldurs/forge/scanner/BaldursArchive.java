package org.baldurs.forge.scanner;

public class BaldursArchive {
    public LocalizationArchive localizationCollector = new LocalizationArchive();
    public StatsArchive.Library statsCollector = new StatsArchive.Library();
    public RootTemplateArchive rootTemplateCollector = new RootTemplateArchive();

    public LocalizationArchive getLocalizationCollector() {
        return localizationCollector;
    }
    public StatsArchive.Library getStatsCollector() {
        return statsCollector;
    }
    public RootTemplateArchive getRootTemplateCollector() {
        return rootTemplateCollector;
    }
}
