package org.bitmagic.ifeed.service.recall;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
public interface RecallHandler {

    List<DocScore> getDocScores();
}
