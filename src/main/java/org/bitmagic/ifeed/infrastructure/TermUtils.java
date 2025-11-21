package org.bitmagic.ifeed.infrastructure;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.rometools.utils.Strings;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yangrd
 * @date 2025/11/19
 **/
public class TermUtils {

    static {
        HanLP.Config.ShowTermNature = false;
    }

    public static List<String> segment(String seq) {
        return HanLP.segment(seq).stream().map(Term::toString).toList();
    }

    public static String segmentStr(String seq) {
        if (Strings.isBlank(seq)) {
            return "";
        }
        return HanLP.segment(seq).stream().map(Term::toString).collect(Collectors.joining(" "));
    }
}
