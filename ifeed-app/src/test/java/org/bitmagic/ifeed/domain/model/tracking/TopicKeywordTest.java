package org.bitmagic.ifeed.domain.model.tracking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicKeywordTest {

    @Test
    void normalizesEnglishCaseAndWhitespaceWithoutMergingDistinctWords() {
        assertEquals("医疗 ai", TopicKeyword.normalize(" \t医疗  AI\n "));
        assertEquals("robot medicine", TopicKeyword.normalize("ROBOT\t\r\nMedicine"));
        assertEquals("", TopicKeyword.normalize(null));
        assertEquals("", TopicKeyword.normalize("\t\n "));
        assertNotEquals(TopicKeyword.normalize("AI"), TopicKeyword.normalize("人工智能"));
        assertNotEquals(TopicKeyword.normalize("OpenAI"), TopicKeyword.normalize("Open AI"));
    }

    @Test
    void lifecycleUpdatesNormalizedKeywordOnCreationAndEdit() {
        TopicTrack track = new TopicTrack();
        track.setKeyword(" 医疗  AI ");
        track.ensureUid();
        var uid = track.getTopicUid();

        assertEquals("医疗 ai", track.getNormalizedKeyword());
        assertNotNull(uid);
        track.setKeyword("ROBOTS");
        track.ensureUid();
        assertEquals("robots", track.getNormalizedKeyword());
        assertEquals(uid, track.getTopicUid());
    }
}
