package org.ensembl.genesearch.test;

import org.elasticsearch.common.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Test behavior of test server according to system properties values
 * (set in gitlab-ci/cd)
 */
public class TestServerTest {

    @Rule
    public final EnvironmentVariables environ = new EnvironmentVariables();

    @Test
    public void testNoEnvironmentSet() {
        assumeTrue(System.getenv("ES_HOST") == null);
        ESTestClient testClient = new ESTestClient();
        assertTrue(testClient.hasContainer());
    }

    @Test
    public void testExternalRunning() {
        assumeTrue(System.getenv("ES_HOST") != null);
        ESTestClient testClient = new ESTestClient();
        // ES test server is "Outside" Test Client - no inner container set as expected
        assertFalse(testClient.hasContainer());
    }

}