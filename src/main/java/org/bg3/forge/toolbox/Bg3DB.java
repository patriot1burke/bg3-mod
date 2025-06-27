package org.bg3.forge.toolbox;

import org.bg3.forge.scanner.Bg3Library;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.nio.file.Path;

@ApplicationScoped
public class Bg3DB {
    private Bg3Library library = new Bg3Library();

    public void start(@Observes StartupEvent event) {
        try {
            Log.info("Starting Bg3DB");

            library.getStatsCollector()
                    .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data/Armor.txt"))
                    .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/Stats/Generated/Data/Armor.txt"))
                    .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/Stats/Generated/Data/Armor.txt"))
                    .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data/Weapon.txt"))
                    .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/Stats/Generated/Data/Weapon.txt"))
                    .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/Stats/Generated/Data/Weapon.txt"));

            library.getRootTemplateCollector().scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/RootTemplates/_merged.lsx"));
            library.getLocalizationCollector().scan(Path.of("/mnt/c/Users/patri/mods/bg3-localization/Localization/English/english.xml"));
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
