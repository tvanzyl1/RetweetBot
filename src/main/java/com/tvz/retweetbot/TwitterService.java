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
    ClassLoader loader = Thread.currentThread().getContextClassLoader();  
    InputStream stream = loader.getResourceAsStream("twitter.properties");
    Properties twitterProps = new Properties();
    try {
        twitterProps.load(stream);
    } catch (IOException ex) {
       logger.error(ex);
    }
    boolean useTwitter = twitterProps.getProperty("twitter_enabled").equalsIgnoreCase("true");
    
    Twitter twitter = null;
    if(useTwitter)        
            twitter = creatTwitterService(twitterProps);
    String datafile = "tweetfile.dat";
    HashMap<String,String> dictionary = buildDictionary();
    
    while(true){
        try {
            try {
                //Read latest tweet
                String latestTweet = "";
                latestTweet = readTweet(twitter, "realDonaldTrump", useTwitter);
                //Check if the tweet was sent previously
                boolean newTweet = latestTweet != null 
                            && isNewTweet(latestTweet, datafile,useTwitter);
                if(newTweet){
                    //Create a new tweet to send.
                    String updatedTweet = letsMakeItInteresting(latestTweet, dictionary);
                    //Send the new tweet out.
                    Status status = null;
                    if(useTwitter)
                     status = twitter.updateStatus(updatedTweet);
                    if(true){ //check status = ok
                        //Write the latest tweet to file
                        writeTweet(latestTweet, datafile, useTwitter);
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
        result = checkAndReplace(result, entry.getKey(), entry.getValue());
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
            String value = pair.substring(index+1, pair.length()).replace("\"", "");
            logger.debug(String.format("%1s %2s", key, value));
            dictionary.put(key, value);
        });
        
    } catch (IOException ex) {
        logger.error(ex);
    }

    return dictionary;
}

private static void writeTweet(String latestTweet,String datafile, 
                                boolean useTwitter){
    
    try {
        List<String> lines = Arrays.asList(latestTweet);
        Path file = Paths.get(datafile);
        if(useTwitter)
        Files.write(file, lines, Charset.forName("UTF-8"));
    } catch (IOException ex) {
        logger.error(ex);
    }
}

private static boolean isNewTweet(String latestTweet, String filename, 
                                boolean useTwitter){
    logger.info("Check if tweet is new ");
    if(!useTwitter) return true;
    
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

private static String readTweet(Twitter twitter, String twitterUser, 
                                boolean useTwitter){
    logger.info("Read tweet");
    String result = null;
    try {
        result = "This is a test tweet that should not be too long.";
        Paging paging = new Paging(1, 100);
        List<Status> statuses;
        if(useTwitter){
            statuses = twitter.getUserTimeline(twitterUser,paging);
            result = statuses.get(0).getText();
        } else {
            result = "America Democrats announce";
        }
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

private static Twitter creatTwitterService(Properties twitterProps){
    Twitter twitter = null;
    TwitterFactory factory = new TwitterFactory();
    AccessToken accessToken = new AccessToken(
                            twitterProps.getProperty("token_key"), 
                            twitterProps.getProperty("token_secret")); 
    twitter = factory.getInstance();
    twitter.setOAuthConsumer(twitterProps.getProperty("consumer_key"), 
                            twitterProps.getProperty("consumer_secret"));
    twitter.setOAuthAccessToken(accessToken);
 
    return twitter;
}

}
