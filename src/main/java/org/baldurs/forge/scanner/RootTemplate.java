package org.baldurs.forge.scanner;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RootTemplate {
    public String Stats;
    public String MapKey;
    public String DisplayName;
    public String Description;
    public String ParentTemplateId;
    public String icon;

    public RootTemplate() {
    }

    public RootTemplate(String stats, String mapKey, String displayName, String description, String parentTemplateId, String icon, RootTemplateArchive archive) {
        Stats = stats;
        MapKey = mapKey;
        DisplayName = displayName;
        Description = description;
        ParentTemplateId = parentTemplateId;
        this.icon = icon;
        this.archive = archive;
    }

    @JsonIgnore
    public RootTemplateArchive archive;

    @JsonIgnore
    public String resolveIcon() {
        if (icon != null) {
            return icon;
        }
        if (ParentTemplateId == null) {
            return null;
        }
        RootTemplate rootTemplate = archive.templates.get(ParentTemplateId);
        if (rootTemplate == null) {
            return null;
        }
        return rootTemplate.resolveIcon();
    }

}
