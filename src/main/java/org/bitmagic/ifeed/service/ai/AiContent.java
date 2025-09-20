package org.bitmagic.ifeed.service.ai;

import java.util.List;

public record AiContent(String summary, String category, List<String> tags, List<Double> embedding) {
}
