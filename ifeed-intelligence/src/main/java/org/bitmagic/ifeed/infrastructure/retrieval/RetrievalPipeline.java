package org.bitmagic.ifeed.infrastructure.retrieval;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
public interface RetrievalPipeline {

   List<DocScore> execute(RetrievalContext context);
}
