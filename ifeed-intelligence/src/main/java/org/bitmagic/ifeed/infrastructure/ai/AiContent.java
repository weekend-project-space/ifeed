package org.bitmagic.ifeed.infrastructure.ai;

import java.util.List;

public record AiContent(String summary, String category, List<String> tags, boolean aiGenerated) {
}
