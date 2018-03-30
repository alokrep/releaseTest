package src.test.java.pstest;

import java.util.ArrayList;

public class TestConfig extends TestPlateService {

    public void testConfigParse() {
        //Ensure we get a 200
        //Parse the protoBuf for env, lteWakeup, telemTransferFreq,logGPSFreq
        //org.junit.Assert.assertTrue( new ArrayList().isEmpty() );
    }

    public void testConfigHash() {
        //Ensure that 200 is returned with full config when hash is empty
        //Ensure that 200 and no config returned when config is not changed
        //Content-Length is 2 in the latter case
    }

}
