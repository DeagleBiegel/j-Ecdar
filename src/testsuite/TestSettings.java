package testsuite;

import logic.SimpleTransitionSystem;

public class TestSettings {
    public final String prefix;
    public final String postfix;
    public final String assertPre;
    public final String assertPost;

    public final String delayPre;

    public final String delayPost;

    public TestSettings(String prefix, String postfix, String assertPre, String assertPost, String delayPre, String delayPost) {
        this.prefix = prefix;
        this.postfix = postfix;
        this.assertPre = assertPre;
        this.assertPost = assertPost;
        this.delayPre = delayPre;
        this.delayPost = delayPost;
    }
}
