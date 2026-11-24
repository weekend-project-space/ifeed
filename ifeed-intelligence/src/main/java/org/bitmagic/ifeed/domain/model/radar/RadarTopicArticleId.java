package org.bitmagic.ifeed.domain.model.radar;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RadarTopicArticleId implements Serializable {

    public String snapshotId;
    public UUID topicId;
    public Long articleId;
}
