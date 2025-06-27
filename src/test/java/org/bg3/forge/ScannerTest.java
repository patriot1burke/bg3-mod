package org.bg3.forge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.bg3.forge.scanner.RootTemplateCollector;
import org.bg3.forge.scanner.StatsCollector;
import org.junit.jupiter.api.Test;


public class ScannerTest {

    //@Test
    public void testEntryScanner() throws IOException {
        StatsCollector.Library library = StatsCollector.scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data/Armor.txt"))
        .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/Stats/Generated/Data/Armor.txt"))
        .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/Stats/Generated/Data/Armor.txt"))
        .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data/Weapon.txt"))
        .scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/SharedDev/Stats/Generated/Data/Weapon.txt"))
        .scan(Path.of("/mnt/c/Users/patri/mods/gustav/Public/GustavDev/Stats/Generated/Data/Weapon.txt"));

        System.out.println("Common attributes: ");
        library.commonAttributes("Armor", "Weapon").stream().sorted().forEach(System.out::println);
    }

    //@Test
    public void testRootTemplateScanner() throws Exception {
        RootTemplateCollector scanner = new RootTemplateCollector();
        scanner.scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/RootTemplates/_merged.lsx"));
        for (RootTemplateCollector.RootTemplate template : scanner.templates.values()) {
            System.out.println(template);
        }
    }
}
