package src.test.java.pstest;

public class TestPerformActions {

    public void testDetachAction() {
        //send perform=DETACH action to perform API
        // expect to received a 200 response code
    }

    public void performAttachAction() {
        //send perform=ATTACH, server should respond back with 200
    }

    public void performAuthCheckAction() {
        //send perform=AUTHORIZED, response code is 200 if authorized(platestatus.attach_authorized = true)
        //response code is 400 if not authorized
    }
}
