package WL.WLhelper;

import java.util.*;

public class ClaimResult {
    //these names dicate the names in the final data
    public final Object claim;

    public ClaimResult(Object claim) {
        this.claim = claim;
    }

    public ClaimResult(Map<String, Object> row) {

        this((Object) row.get("claim"));
    }
}