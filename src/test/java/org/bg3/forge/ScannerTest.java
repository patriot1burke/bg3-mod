package org.bg3.forge;

import java.io.IOException;
import java.nio.file.Path;

import org.baldurs.forge.scanner.IconCollector;
import org.baldurs.forge.scanner.RootTemplate;
import org.baldurs.forge.scanner.RootTemplateArchive;
import org.baldurs.forge.scanner.StatsArchive;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;


public class ScannerTest {
    //@Test
    public void testObjecvtMapper() throws Exception{
        String hello = "hello \"world\"";
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(hello));

    }

    //@Test
    public void testIconExtractor() throws Exception {
        /*
        IconCollector.extractIcons("/mnt/c/Users/patri/mods/shared/Public/SharedDev/GUI/Icons_Skills.lsx",
        "/mnt/c/Users/patri/mods/icons/Public/SharedDev/Assets/Textures/Icons/Icons_Skills.dds",
        "/home/bburke/projects/bg3-forge/icons/skills"
        );
        IconCollector.extractIcons("/mnt/c/Users/patri/mods/shared/Public/Shared/GUI/Icons_Skills.lsx",
        "/mnt/c/Users/patri/mods/icons/Public/Shared/Assets/Textures/Icons/Icons_Skills.dds",
        "/home/bburke/projects/bg3-forge/icons/skills"
        );
        IconCollector.extractIcons("/mnt/c/Users/patri/mods/shared/Public/Shared/GUI/Icons_Items.lsx",
        "/mnt/c/Users/patri/mods/icons/Public/Shared/Assets/Textures/Icons/Icons_Items.dds",
        "/home/bburke/projects/bg3-forge/icons/items"
        );
        IconCollector.extractIcons("/mnt/c/Users/patri/mods/shared/Public/Shared/GUI/Icons_Items_2.lsx",
        "/mnt/c/Users/patri/mods/icons/Public/Shared/Assets/Textures/Icons/Icons_Items_2.dds",
        "/home/bburke/projects/bg3-forge/icons/items"
        );
        IconCollector.extractIcons("/mnt/c/Users/patri/mods/shared/Public/Shared/GUI/Icons_Items_3.lsx",
        "/mnt/c/Users/patri/mods/icons/Public/Shared/Assets/Textures/Icons/Icons_Items_3.dds",
        "/home/bburke/projects/bg3-forge/icons/items"
        );
        IconCollector.extractIcons("/mnt/c/Users/patri/mods/shared/Public/Shared/GUI/Icons_Items_4.lsx",
        "/mnt/c/Users/patri/mods/icons/Public/Shared/Assets/Textures/Icons/Icons_Items_4.dds",
        "/home/bburke/projects/bg3-forge/icons/items"
        );
        IconCollector.extractIcons("/mnt/c/Users/patri/mods/shared/Public/Shared/GUI/Icons_Items_5.lsx",
        "/mnt/c/Users/patri/mods/icons/Public/Shared/Assets/Textures/Icons/Icons_Items_5.dds",
        "/home/bburke/projects/bg3-forge/icons/items"
        );
        IconCollector.extractIcons("/mnt/c/Users/patri/mods/shared/Public/Shared/GUI/Icons_Items_6.lsx",
        "/mnt/c/Users/patri/mods/icons/Public/Shared/Assets/Textures/Icons/Icons_Items_6.dds",
        "/home/bburke/projects/bg3-forge/icons/items"
        );
        */
    }


    //@Test
    public void testEntryScanner() throws IOException {
        StatsArchive.Library library = StatsArchive.scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/Stats/Generated/Data/Armor.txt"))
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
        RootTemplateArchive scanner = new RootTemplateArchive();
        scanner.scan(Path.of("/mnt/c/Users/patri/mods/shared/Public/Shared/RootTemplates/_merged.lsx"));
        for (RootTemplate template : scanner.templates.values()) {
            System.out.println(template);
        }
    }
}
