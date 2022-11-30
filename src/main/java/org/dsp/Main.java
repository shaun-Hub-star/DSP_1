package org.dsp;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.dsp.S3_service.S3Instance;
import software.amazon.awssdk.regions.Region;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;


public class Main {
    public static void main(String[] args) throws IOException {

        /*String links_path = "src/main/resources/image_links.txt";
        List<String> images_link_ = parseImagesLinks(links_path);
        List<String> images_link = new ArrayList<>(
                new HashSet<>(images_link_));
        List<Pair<String, String>> imgLinkOCRs = new LinkedList<>();
        for (String imgLink : images_link) {
            imgLinkOCRs.add(new Pair<>(imgLink, doOCR(downloadImage(imgLink))));
        }
        createHTML(createHtmlBody(imgLinkOCRs), "src/main/resources/Output");*/

        String suffix = "0525381648dqw4w9wgxcq";
        String jarsBucket = "jars" + suffix;
        Region region = Region.US_EAST_1;
        S3Instance s3Jars = new S3Instance(region, jarsBucket);
        s3Jars.createBucket();
        String managerJarKey = "ManagerJar";
        String managerJarPath = "/home/spl-labs/Desktop/DSP_213/out/artifacts/DSP_213_jar/DSP_213.jar";
        s3Jars.uploadFile(managerJarKey, managerJarPath);


    }

    public static List<String> parseImagesLinks(String path) {
        List<String> links = new LinkedList<>();
        try (FileReader fr = new FileReader(path)) {
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            String line;
            while ((line = br.readLine()) != null) {
                links.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    private static Pair<String, String> getLinkNameAndFormat(String linkName) {
        String[] split = linkName.split("/");
        String name = split[split.length - 1];
        return new Pair<>(name /*name*/, name.split("\\.")[1] /*format*/);//if the time allows to do format checking because the ocr supports certain formats
    }

    private static File downloadImage(String link) throws IOException {
        URL url = new URL(link);
        BufferedImage img;
        try {
            img = ImageIO.read(url);
            Pair<String, String> nameFormat = getLinkNameAndFormat(link);
            File file = new File("src/main/resources/Images/" + nameFormat.getFirst());
            ImageIO.write(img, nameFormat.getSecond(), file);
            return file;
        } catch (Exception e) {
            return null;
        }

    }

    private static String doOCR(File img) {
        if (img == null)
            return "Error: failed to download the img";

        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("src/main/resources/tessdata");
            return tesseract.doOCR(img);
        } catch (TesseractException e) {
            return "Error: " + e.getMessage();
        }

    }





}
