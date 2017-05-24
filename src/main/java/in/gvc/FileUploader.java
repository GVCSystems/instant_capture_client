package in.gvc;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;


/**
 * Created by arpit on 12/5/17.
 */
public class FileUploader {


    Logger logger = LoggerFactory.getLogger(FileUploader.class);

    private static String executeRequest(HttpRequestBase requestBase){
        String responseString = "" ;

        InputStream responseStream = null ;
        HttpClient client = new DefaultHttpClient() ;
        try{
            HttpResponse response = client.execute(requestBase) ;
            if (response != null){
                HttpEntity responseEntity = response.getEntity() ;

                if (responseEntity != null){
                    responseStream = responseEntity.getContent() ;
                    if (responseStream != null){
                        BufferedReader br = new BufferedReader (new InputStreamReader (responseStream)) ;
                        String responseLine = br.readLine() ;
                        String tempResponseString = "" ;
                        while (responseLine != null){
                            tempResponseString = tempResponseString + responseLine + System.getProperty("line.separator") ;
                            responseLine = br.readLine() ;
                        }
                        br.close() ;
                        if (tempResponseString.length() > 0){
                            responseString = tempResponseString ;
                        }
                    }
                }
            }
        } catch (Exception e) {
		System.out.println("error in server");

        } finally{
            if (responseStream != null){
                try {
                    responseStream.close() ;
                } catch (IOException e) {

                }
            }
        }
        client.getConnectionManager().shutdown();

        return responseString ;
    }

    public String executeMultiPartRequest(String urlString, File file) {

        HttpPost postRequest = new HttpPost (urlString) ;

            MultipartEntity multiPartEntity = new MultipartEntity() ;

            FileBody fileBody = new FileBody(file, "application/octect-stream") ;
            multiPartEntity.addPart("file", fileBody) ;

            postRequest.setEntity(multiPartEntity) ;

        return executeRequest (postRequest) ;
    }

    ArrayList<String> arrayList = new ArrayList<String>();
    int running = 0;
    int run_left=0;

    public void uploadIt()
    {
        if(running == 0)
            while(run_left > 0)
            {
                running=1;
                File file = new File(arrayList.get(0));
                String response = executeMultiPartRequest("http://159.203.112.66:80/upload", file) ;
                System.out.println(response);
                if(response.contains("successfully")) {
                    arrayList.remove(0);
                    logger.info("Uploaded the file "+file.getName());
                    run_left--;
                    file.delete();
                }
                if(run_left == 0) {
                    running = 0;
                    return;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                }
            }
    }

}

