package org.ensembl.genesearch.test;

import org.ensembl.genesearch.test.ESTestClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

import static org.junit.Assert.*;

/**
 * Test behavior of test server according to system properties values
 * (set in gitlab-ci/cd)
 */
public class TestServerTest {

    @Rule
    public final EnvironmentVariables environ = new EnvironmentVariables();

    @Test
    public void testDockerEnabled () {
        environ.set("TEST_PROFILE", "local");
        assertEquals("local", System.getenv("TEST_PROFILE"));
        ESTestClient testClient = new ESTestClient();
        assertTrue(testClient.hasContainer());
    }

    @Test(expected = RuntimeException.class)
    public void testRemoteException () {
        environ.set("TEST_PROFILE", "integration");
        assertEquals("integration", System.getenv("TEST_PROFILE"));
        ESTestClient testClient = new ESTestClient();
        assertTrue(testClient.hasContainer());
    }

    @Test(expected = RuntimeException.class)
    public void testIntegrationNoDocker () {
        environ.set("TEST_PROFILE", "integration");
        assertEquals("integration", System.getenv("TEST_PROFILE"));
        ESTestClient testClient = new ESTestClient();
        assertFalse(testClient.hasContainer());
    }

}