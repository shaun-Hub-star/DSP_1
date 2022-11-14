package org.example;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


public class Main {
    public static void main(String[] args) throws  IOException {

        String links_path = "src/main/resources/image_links.txt";
        List<String> images_link_ = parseImagesLinks(links_path);
        List<String> images_link = new ArrayList<>(
                new HashSet<>(images_link_));
        List<Pair<String, String>> imgLinkOCRs = new LinkedList<>();
        for(String imgLink : images_link){
            imgLinkOCRs.add(new Pair<>(imgLink, doOCR(downloadImage(imgLink))));
        }
        createHTML(createHtmlBody(imgLinkOCRs), "src/main/resources/Output");

    }

    private static List<String> parseImagesLinks(String path) {
        List<String> links = new LinkedList<>();
        try(FileReader fr = new FileReader(path)) {
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
    private static Pair<String, String> getLinkNameAndFormat(String linkName){
        var split = linkName.split("/");
        String name = split[split.length - 1];
        return new Pair<>(name, name.split("\\.")[1]);
    }
    private static File downloadImage(String link) throws IOException {
        URL url = new URL(link);
        BufferedImage img;
        try {
            img = ImageIO.read(url);
            Pair<String, String> nameFormat = getLinkNameAndFormat(link);
            File file = new File("src/main/resources/Images/"+nameFormat.getFirst());
            ImageIO.write(img, nameFormat.getSecond(), file);
            return file;
        } catch (Exception e) {
            return null;
        }

    }

    private static String doOCR(File img) {
        if(img == null)
            return "Error: failed to download the img";

        try{
          ITesseract tesseract = new Tesseract();
                  tesseract.setDatapath("src/main/resources/tessdata");
                  return tesseract.doOCR(img);
        }
        catch (TesseractException e){
            return "Error: " + e.getMessage();
        }

    }

    private static String createHtmlBody(List<Pair<String,String>> imgLinkOCRs){
        StringBuilder body = new StringBuilder();
        for(Pair<String, String> img : imgLinkOCRs){
            body.append("<p>\n\t<img src=\"")
                .append(img.getFirst())
                .append("\"><br>\n\t")
                .append(img.getSecond())
                .append("\n</p>\n");
        }
        return body.toString();
    }

    private static void createHTML(String htmlBody, String dirPath){
        String html =
                """
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
                    <title>OCR</title>
                </head>
                """ + htmlBody + "</html>";

        try(FileWriter fw = new FileWriter(dirPath + "\\results.html")) {
            fw.write(html);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
