package src.test.java.pstest;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public abstract class TestPlateService {
    private String serialNo;
    private String env;

    private CloseableHttpResponse runHttpGet(String url) {
        CloseableHttpClient hClient = HttpClients.createDefault();
        String auth = serialNo + ":" + serialNo;
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response1 = null;
        byte[] encodedAuth = Base64.encodeBase64(
                //auth.getBytes(Charset.forName("ISO-8859-1")));
                auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        try {
            response1 = hClient.execute(httpGet);
            System.out.println(String.format("GET on %s returned response code %s",url,response1.getStatusLine()));

            return response1;
        } catch(IOException iox) {
            System.err.println("IOException occurred while executing GET on "+url);
            iox.printStackTrace();
        }
        return null;
    }
}
