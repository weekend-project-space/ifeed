package org.bitmagic.ifeed.domain.model.tracking;

public final class TopicKeyword {

    private TopicKeyword() {}

    public static String normalize(String keyword) {
        if (keyword == null) return "";
        String collapsed = keyword.replaceAll("[ \\t\\n\\r\\f\\x0B]+", " ").trim();
        StringBuilder normalized = new StringBuilder(collapsed.length());
        for (int index = 0; index < collapsed.length(); index++) {
            char character = collapsed.charAt(index);
            normalized.append(character >= 'A' && character <= 'Z' ? (char) (character + ('a' - 'A')) : character);
        }
        return normalized.toString();
    }
}
