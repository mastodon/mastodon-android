package org.joinmastodon.android.model;

import java.util.List;

public class EmojiCategory {
    public String title;
    public List<Emoji> emojis;

    public EmojiCategory(String title, List<Emoji> emojis) {
        this.title = title;
        this.emojis = emojis;
    }
}
