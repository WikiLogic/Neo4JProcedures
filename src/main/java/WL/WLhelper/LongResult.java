
package WL.WLhelper;


public class LongResult {
    public final static LongResult NULL = new LongResult(null);
    public final Long value;

    public LongResult(Long value) {
        this.value = value;
    }
}