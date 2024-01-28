package com.fatih.translator;


import com.lowagie.text.DocumentException;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.springframework.stereotype.Component;


import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class Translator {
    private Map<String,String> visitedLinks;
    private Queue<String> willVisitList ;

    private final String directoryPath = "";

    private final static String translateUrl= "";

    private final String[] excludePaths = {".jpg"};

    private final String mainUrlRegex = "^https://www\\.deneme\\.com/.*$";

@PostConstruct
public void getAllPagesAndTranslate() {
    visitedLinks = new HashMap<>();
    willVisitList = new LinkedList<>();

    String filePath = directoryPath + "links.txt"; // Replace with the actual file path

    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
        String line;

        while ((line = reader.readLine()) != null) {
            if(!visitedLinks.containsKey(line)) {
                willVisitList.add(line);
            }
            while (!willVisitList.isEmpty()) {
                String name = willVisitList.remove();
                System.out.println(name);
                processPage(name);
            }
            System.out.println(line + " CHECKED.");
        }
    } catch (IOException e) {
        e.printStackTrace();
    }



        }
        private boolean checkExclude(String url){
            for (String path:excludePaths) {
                if (url.contains(path))
                    return false;
            }
            return true;
        }

        public boolean processPage(String url){

            try {
                Document document = Jsoup.connect(url).get();
                String pageTitle = document.title();

                String path = getPathFromUrl(url);
                path = path.replace("/","\\");
                // Create a directory based on the path
                File directory = new File(directoryPath + path);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                visitedLinks.put(url,directory.getAbsolutePath());
                Elements hrefLinks = document.select("a[href]");
                for (Element link : hrefLinks) {
                    if(!willVisitList.contains(link.attr("href")) && !visitedLinks.containsKey(link.attr("href")) && link.attr("href").matches(mainUrlRegex) && checkExclude(link.attr("href"))){
                        willVisitList.add(link.attr("href"));
                    }
                    link.removeAttr("href");
                }
                if(checkFileExistenceUsingExists(directory.getAbsolutePath() +"\\"+pageTitle +".html")){
                    return true;
                }
                // Translate paragraphs
                Elements paragraphs = document.select("p");
                translateAndReplace(paragraphs, "en", "tr");

                // Translate headings
                Elements headings = document.select("h1, h2, h3, h4, h5, h6");
                translateAndReplace(headings, "en", "tr");


                saveToFile(directory.getAbsolutePath() +"\\"+pageTitle +".html",document.outerHtml());
return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

    private static String getPathFromUrl(String url) {
        try {
            URL urla = new URL(url);
            String[] pathSegments = urla.getPath().split("/");

            // Find the first non-empty directory
            for (String segment : pathSegments) {
                if (!segment.isEmpty()) {
                    return "/" + segment + "/";
                }
            }

            // If no directory is found, return an empty string
            return "";
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    private  boolean checkFileExistenceUsingExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }
    private static void saveToFile(String fileName, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        private static void translateAndReplace(Elements elements, String sourceLanguage, String targetLanguage) {
            for (Element element : elements) {
                String originalText = element.text();
                String translatedText = translateUsingGoogleTranslate(originalText, sourceLanguage, targetLanguage);
                if (translatedText != null) {
                    element.text(translatedText);
                }
            }
        }

        private static String translateUsingGoogleTranslate(String text, String sourceLanguage, String targetLanguage) {
            try {
                String url = translateUrl + targetLanguage + "&sl=" + sourceLanguage + "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

                Document translationDocument = Jsoup.connect(url).get();
                Elements resultElements = translationDocument.select(".result-container");

                // Extract text from each result element
                StringBuilder translatedText = new StringBuilder();
                for (Element resultElement : resultElements) {
                    translatedText.append(resultElement.text()).append(" ");
                }

                return translatedText.toString().trim();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }



}
