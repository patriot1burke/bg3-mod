package org.baldurs.forge.scanner;

public record RootTemplate(String Stats, String MapKey, String DisplayName, String Description, String ParentTemplateId, String icon, RootTemplateArchive archive) {

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
