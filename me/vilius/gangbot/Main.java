package me.vilius.gangbot;

/*
 *   Written by yours truly - Nya (Vilius)
 *   Created 2019-09-13
 *   Inspired by Flex Seal™
 */
public class Main
{
    public static void main(String[] args) throws Exception {
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        new BotDriver().executeBot();
    }
}
