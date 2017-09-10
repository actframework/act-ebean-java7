package act.db.ebean;

import org.junit.Test;
import org.osgl.ut.TestBase;

public class VersionTest extends TestBase {

    @Test
    public void versionShallContainsEbean() {
        yes(EbeanPlugin.VERSION.toString().contains("ebean"));
    }

}
