package com.tvz.retweetbot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.*;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class TwitterService extends Thread{

final static Logger logger = Logger.getLogger(TwitterService.class);

public TwitterService(){
    super();
    logger.info("Twitter service initialising");
}

@Override
public void run(){
    logger.info("Twitter service executing");
    
    Twitter twitter = creatTwitterService();
    String datafile = "tweetfile.dat";
    HashMap<String,String> dictionary = buildDictionary();
    
    while(true){
        try {
            try {
                //Read latest tweet
                String latestTweet = readTweet(twitter, "realDonaldTrump");
                //Check if the tweet was sent previously
                boolean newTweet = latestTweet != null 
                            && isNewTweet(latestTweet, datafile);
                if(newTweet){
                    //Create a new tweet to send.
                    String updatedTweet = letsMakeItInteresting(latestTweet, dictionary);
                    //Send the new tweet out.
                    Status status = twitter.updateStatus(updatedTweet);
                    if(true){ //check status = ok
                        //Write the latest tweet to file
                        writeTweet(latestTweet, datafile);
                    }
                }
                //logger.info("Successfully updated the status to [" + status + "].");
            } catch (Exception ex){//TwitterException ex) {
                logger.error(ex);
            }
            
            Thread.sleep(120000);
            logger.info("Twitter service executed");
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(TwitterService.class.getName()).log(java.util.logging.Level.SEVERE, null,ex);
        }
    }
  

}

private static String letsMakeItInteresting(String latestTweet, 
                                            HashMap<String, String> dictionary){
    logger.debug("Making it interesting.");
    String result = latestTweet;
    latestTweet = latestTweet.replace("&amp;", "or");
    for(Map.Entry<String, String> entry : dictionary.entrySet()) {
        result = checkAndReplace(latestTweet, entry.getKey(), entry.getValue());
    }
    if (result.length() + "@realDonaldTrump ".length() <= 140) result = 
                "@realDonaldTrump " + result;
    result = footerText(result);
    logger.info("New tweet :"+result);
    
    return result;
}

public static String checkAndReplace(String text, String origText, String newText)
{
    return text.length() - origText.length() + newText.length() <= 140 ? text.replace(origText, newText) : text;
}

private static HashMap<String, String> buildDictionary(){
    logger.info("Building dictionary");
    
    HashMap<String, String> dictionary = new HashMap<>();
    try {
        List<String> contents = (Files.readAllLines(Paths.get("dictionary.dict")));
        contents.forEach((pair) -> {
            int index = pair.indexOf(",");
            String key = pair.substring(0, index).replace("\"", "");
            String value = pair.substring(index, pair.length()).replace("\"", "");
            dictionary.put(key, value);
        });
        
    } catch (IOException ex) {
        logger.error(ex);
    }

    return dictionary;
}

private static void writeTweet(String latestTweet,String datafile){
    try {
        List<String> lines = Arrays.asList(latestTweet);
        Path file = Paths.get(datafile);
        Files.write(file, lines, Charset.forName("UTF-8"));
    } catch (IOException ex) {
        logger.error(ex);
    }
}

private static boolean isNewTweet(String latestTweet, String filename){
    logger.info("Check if tweet is new ");
    boolean result = false;
    try {
        File file = new File(filename);
        //if a file doesn't exist create a new one.
        if(!file.exists()) 
            file.createNewFile();
        String contents = new String(Files.readAllBytes(Paths.get(filename)));
        logger.debug("old:"+contents);
        logger.debug("new:"+latestTweet);
        result = !contents.trim().equalsIgnoreCase(latestTweet.trim());
    } catch (IOException ex) {
        logger.error(ex);
    }
    //check 
    logger.debug("Is tweet new :"+result);
    return result;
}

private static String readTweet(Twitter twitter, String twitterUser){
    logger.info("Read tweet");
    String result = null;
    try {
        result = "This is a test tweet that should not be too long.";
        Paging paging = new Paging(1, 100);
        List<Status> statuses;
        statuses = twitter.getUserTimeline(twitterUser,paging);
        result = statuses.get(0).getText();
    } catch (TwitterException ex) {
        logger.error(ex);
    }
    
    logger.info("Old tweet :"+result);
    return result;
}

private static String footerText(String text)
{
    List<String> sayings = Arrays.asList(  " We won!",
                                        " Who da man!",
                                        " I'm awesome!",
                                        " Great people!",
                                        " Gonna be huge!",
                                        " I own them now!",
                                        " Show me the money!",
                                        " Make America BREED again!");
    Collections.reverse(sayings);
    for(String str : sayings){
        if (text.length() + str.length() <= 140)
        {
            text = text + str;
            return text;
        }
    }
    return text;
}

private static Twitter creatTwitterService(){
    Twitter twitter = null;
    try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();  
        InputStream stream = loader.getResourceAsStream("twitter.properties");
        Properties twitterProps = new Properties();

        twitterProps.load(stream);

        TwitterFactory factory = new TwitterFactory();
        AccessToken accessToken = new AccessToken(
                                twitterProps.getProperty("token_key"), 
                                twitterProps.getProperty("token_secret")); 
        twitter = factory.getInstance();
        twitter.setOAuthConsumer(twitterProps.getProperty("consumer_key"), 
                                twitterProps.getProperty("consumer_secret"));
        twitter.setOAuthAccessToken(accessToken);
    } catch (IOException ex) {
        logger.error(ex);
    }    
    return twitter;
}

}
