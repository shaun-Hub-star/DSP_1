import org.dsp.Worker.Worker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class WorkerTests {

    @Test
    public void OCR_Test(){
        String imageLink = "http://luc.devroye.org/OCR-A-Comparison-2009.jpg";

        try{
            File img = Worker.downloadImage(imageLink);
            String results = Worker.doOCR(img);
            System.out.println("results = " + results);
        }
        catch (Exception e){
            Assertions.fail();
        }
    }
}
