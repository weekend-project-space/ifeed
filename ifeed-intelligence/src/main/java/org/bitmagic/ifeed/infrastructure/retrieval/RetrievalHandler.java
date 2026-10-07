package org.bitmagic.ifeed.infrastructure.retrieval;

import java.util.List;

/**
 * @author yangrd
 * @date 2025/11/3
 **/
public interface RetrievalHandler {

    boolean supports(RetrievalContext context);

    List<DocScore> handle(RetrievalContext context);
}
