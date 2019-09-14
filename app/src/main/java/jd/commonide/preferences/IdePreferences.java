package jd.commonide.preferences;

import jd.common.preferences.CommonPreferences;

public class IdePreferences
    extends CommonPreferences {
    protected boolean showMetadata;

    public IdePreferences() {
        this.showMetadata = true;
    }

    public IdePreferences(boolean showDefaultConstructor, boolean realignmentLineNumber, boolean showPrefixThis, boolean mergeEmptyLines, boolean unicodeEscape, boolean showLineNumbers, boolean showMetadata) {
        super(showDefaultConstructor, realignmentLineNumber, showPrefixThis, mergeEmptyLines, unicodeEscape, showLineNumbers);
        this.showMetadata = showMetadata;
    }

    public boolean isShowMetadata() {
        return this.showMetadata;
    }
}
