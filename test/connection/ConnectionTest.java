package connection;

import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class ConnectionTest {
    @Test
    public void testVersion() {
        assertEquals(Main.ENGINE_NAME + " Version: " + Main.VERSION, (Main.chooseCommand("-version")));
    }

    @Test
    public void testHelp() {
        assertEquals("In order to check version type:-version\n" +
                "In order to run query type:-rq folderPath query query...\n" +
                "In order to check the validity of a query type:-vq query", (Main.chooseCommand("-help")));
    }

    @Test
    public void testVerificationOfQuery() {
        assertEquals("true", (Main.chooseCommand("-vq refinement:Spec<=Spec")));
    }

    @Test
    public void testRunSingleQuery1() {
        assertEquals("true", (Main.chooseCommand("-rq ./samples/EcdarUniversity refinement:Spec<=Spec")));
    }

    @Test
    public void testRunSingleQuery2() {
        assertEquals("true", (Main.chooseCommand("-rq ./samples/EcdarUniversity refinement:(Administration||Machine||Researcher)<=Spec")));
    }

    @Test
    public void testRunSingleQuery3() {
        assertEquals("true", (Main.chooseCommand("-rq ./samples/EcdarUniversity refinement:(HalfAdm1&&HalfAdm2)<=Adm2")));
    }

    @Test
    public void testRunMultipleQueries() {
        String query = "-rq ./samples/EcdarUniversity refinement:spec<=spec refinement:Machine<=Machine";
        assertEquals("true true", (Main.chooseCommand(query)));
    }

    @Test
    public void testRunMultipleQueries2() {
        String query = "-rq ./samples/EcdarUniversity refinement:(Administration||Machine||Researcher)<=Spec refinement:Machine3<=Machine3";
        assertEquals("true true", (Main.chooseCommand(query)));
    }

    @Test
    public void testRunMultipleQueries3() {
        String query = "-rq ./samples/EcdarUniversity refinement:Spec<=(Administration||Machine||Researcher) refinement:Machine3<=Machine3";
        assertEquals("false true", (Main.chooseCommand(query)));
    }

    @Test
    public void testRunMultipleQueries4() {
        String query = "-rq ./samples/EcdarUniversity refinement:Spec<=Spec refinement:Machine<=Machine refinement:Machine3<=Machine3 refinement:Researcher<=Researcher";
        assertEquals("true true true true", (Main.chooseCommand(query)));
    }
    @Test
    public void testRunInvalidQuery() {
        assertEquals("Error: null", (Main.chooseCommand("-rq sdfsd xcv")));
    }

    @Test
    public void testRunInvalidQuery2() {
        String expected = "Unknown command: \"-machine 1 2 3\"\nwrite -help to get list of commands";
        String result = Main.chooseCommand("-machine 1 2 3");
        assertEquals(result, expected);
    }

    @Test
    public void testValidateInvalidQuery1() {
        assertEquals("Error: Expected: \"refinement:\"", (Main.chooseCommand("-vq spec<=spec")));
    }

    @Test
    public void testValidateInvalidQuery2() {
        assertEquals("Error: Incorrect syntax, does not contain any feature", (Main.chooseCommand("-vq sdf")));
    }
}