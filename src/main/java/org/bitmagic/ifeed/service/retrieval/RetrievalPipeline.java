package org.bitmagic.ifeed.service.retrieval;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
public interface RetrievalPipeline {

   List<Long> execute(RetrievalContext context);
}
