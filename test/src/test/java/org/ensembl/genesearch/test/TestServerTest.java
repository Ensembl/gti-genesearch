package org.ensembl.genesearch.test;

import org.elasticsearch.common.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Test behavior of test server according to system properties values
 * (set in gitlab-ci/cd)
 */
public class TestServerTest {

    @Rule
    public final EnvironmentVariables environ = new EnvironmentVariables();

    @Test
    public void testNoEnvironmentSet() throws Exception {
        assumeTrue(System.getenv("ES_HOST") == null);
        ESTestClient testClient = new ESTestClient();
        assertNotNull(testClient.getClient());
    }

    @Test
    public void testExternalRunning() throws Exception {
        assumeTrue(System.getenv("ES_HOST") != null);
        ESTestClient testClient = new ESTestClient();
        // ES test server is "Outside" Test Client - no inner container set as expected
        assertNotNull(testClient.getClient());
    }

}