package me.vilius.gangbot;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/*
 *   Written by yours truly - Nya (Vilius)
 *   Created 2019-09-13
 *   Inspired by Flex Seal™
 */
public class BotDriver extends JFrame /*why*/ {

    //TODO: make this load from some config?
    private final boolean RUBY_FARMING = true;

    JLabel title = new JLabel("Gangbot by Nya");
    JLabel harvestStatus = new JLabel("Planting...");

    private final String USERNAME = "", PASSWORD = "";

    private WebDriver driver;

    private JavascriptExecutor executor;

    private Random rng = new Random();

    private Tesseract tess;

    public void executeBot() throws Exception {
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        ChromeOptions options = new ChromeOptions();
        //Headless doesn't work, we need the dom
        //options.addArguments("--headless");
        //Create our tess4j for captcha recognition
        tess = new Tesseract();
        tess.setDatapath(System.getProperty("user.dir"));
        tess.setLanguage("eng");
        //Captcha will only contain numbers
        tess.setTessVariable("tessedit_char_whitelist", "0123456789");
        //Navigate to the page of the game
        this.driver.get("https://gangsteriai.lt/Main/main.php");

        //Resize the window
        driver.manage().window().setSize(Toolkit.getDefaultToolkit().getScreenSize());

        //Login
        this.driver.findElement(By.name("username")).sendKeys(USERNAME);
        this.driver.findElement(By.name("password")).sendKeys(PASSWORD);
        this.driver.findElement(By.cssSelector(".btn-login")).click();

        Thread.sleep(15000);

        //Run bot
        while (true) {
            //botTick();
            handleBrothel();
        }
    }

    public BufferedImage captcha(WebElement captcha) throws IOException {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        BufferedImage fullImg = ImageIO.read(screenshot);
        Point point = captcha.getLocation();
        int eleWidth = captcha.getSize().getWidth();
        int eleHeight = captcha.getSize().getHeight();
        BufferedImage eleScreenshot = fullImg.getSubimage(point.getX(), point.getY(),
            eleWidth, eleHeight);
        return eleScreenshot;
    }

	//Epic exploit, the game still lets us do most tasks despite us being in the hospital
    private void bypassHospital() {
        while (true) {
            if (driver.findElements(By.className("disabled-map")).size() != 0) {
                executor.executeScript("return document.getElementsByClassName('disabled-map')[0].remove();");
            }
        }
    }

    private void botTick() throws InterruptedException, TesseractException {
        System.out.println("ticking the bot");

        WebDriverWait wait = new WebDriverWait(driver, 10);

        //handleBrothel();

        int maxLand, seedCount; {
            int money;
            //Make sure we get the growhouse first
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("area:nth-child(3)")));
            WebElement growhouse = driver.findElement(By.cssSelector("area:nth-child(3)"));
            //Open the growhouse
            if (driver.findElements(By.className("disabled-map")).size() != 0) {
                return;
            }
            growhouse.click();
            Thread.sleep(1000);
            System.out.println("blah");
            String str = ((String) executor.executeScript("return document.getElementsByClassName(\"help-text help\")[0].textContent;")).replaceAll(",", "").replaceAll("[^0-9]+", " ");
            List <String> ohno = Arrays.asList(str.trim().split(" "));
            maxLand = Integer.parseInt(ohno.get(0));
            seedCount = Integer.parseInt(ohno.get(1));

            System.out.println(seedCount);

            if (seedCount < 100) { //Need to buy some seeds first
                System.out.println("buying seeds");

                //Select the seed screen
                WebElement seeds = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, 'building_agriculture_potseeds.php')]")));
                seeds.click();
                money = Integer.parseInt(driver.findElement(By.cssSelector(".renew-money")).getText().replaceAll("[^0-9]", ""));
                int maxSeeds = RUBY_FARMING ? 100 : (int) Math.min(Math.floor(money / 3), maxLand * 100);
                //Wait for seed field to fully appear, set it's text
                System.out.println("waiting: " + maxSeeds);
                Thread.sleep(2000);
                //Weird and hacky, fix it later maybe
                executor.executeScript("document.getElementsByName('amount')[0].value=" + maxSeeds);
                System.out.println("set seeds to buy to: " + maxSeeds);
                //Hit buy
                WebElement buy = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-link")));
                buy.click();
            }
        }

        Thread.sleep(1000);

        //Grow and harvest
        {
            WebElement land = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, 'building_agriculture_manageland.php')]")));
            land.click();

            String captcha = "";

            while (true) {
                Thread.sleep(500);
                //Refresh the captcha
                land = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@href, 'building_agriculture_manageland.php')]")));
                land.click();
                Thread.sleep(700);
                //Solve the captcha with ocr and enter
                if (driver.findElements(By.cssSelector(".captcha-img")).size() == 0) {
                    continue;
                }
                WebElement c = driver.findElement(By.cssSelector(".captcha-img"));

                BufferedImage saveImage;

                try {
                    saveImage = captcha(c);
                } catch (Exception e) {
                    continue;
                }

                captcha = tess.doOCR(saveImage);
                captcha = captcha.replaceAll("\n", "").replaceAll(" ", "");

                System.out.println("captcha readin: " + captcha);

                if (captcha.length() == 3) {
                    //Get owned captcha
                    if (driver.findElements(By.cssSelector(".captcha-img")).size() == 0) {
                        continue;
                    }
                    executor.executeScript("document.getElementsByName('code')[0].value=" + captcha);
                    //Find seed count and max land
                    System.out.println("blah");
                    String str = ((String) executor.executeScript("return document.getElementsByClassName(\"help-text help\")[0].textContent;")).replaceAll(",", "").replaceAll("[^0-9]+", " ");
                    List <String> ohno = Arrays.asList(str.trim().split(" "));
                    maxLand = Integer.parseInt(ohno.get(0));
                    seedCount = Integer.parseInt(ohno.get(1));
                    int plantable = RUBY_FARMING ? 1 : (int) Math.floor(Math.min(maxLand * 100, seedCount) / 100);
                    //Set land to plant
                    executor.executeScript("document.getElementsByName('amount')[0].value=" + plantable);
                    //Plant
                    executor.executeScript("document.getElementsByClassName('btn btn-green drop-shadow drop-shadow-hover drop-shadow-active')[0].click()");
                    Thread.sleep(1500);
                    //See if we really planted or maybe the reading messed up, if it did, try again:
                    try {
                        if (((String) executor.executeScript("return document.getElementsByClassName('entry drop-shadow drop-shadow-hover alone growing')[0].textContent;")).contains("auginama"))
                            break;
                        else
                            continue;
                    } catch (Exception e) {
                        continue;
                    }
                } else {
                    continue;
                }
            }
            //Harvest button (wait for it to appear)
            while (!(((String) executor.executeScript("return document.getElementsByClassName('entry drop-shadow drop-shadow-hover alone growing')[0].textContent;")).contains("pribrendo"))) {
                Thread.sleep(3500);
                if (!RUBY_FARMING) {
                    String[] timeUntilHarvest = driver.findElement(By.cssSelector(".working")).getText().replaceAll("\n", "").replace(" ", "").split(":");
                    long timeUntilHarvestMillis = (Long.parseLong(timeUntilHarvest[0]) * 3600000 + Long.parseLong(timeUntilHarvest[1]) * 60000 + Long.parseLong(timeUntilHarvest[2]) * 1000) + 1000;
                    handleSkilling(timeUntilHarvestMillis);
                    //handleBrothel(timeUntilHarvestMillis);
                }
                if ((((String) executor.executeScript("return document.getElementsByClassName('entry drop-shadow drop-shadow-hover alone growing')[0].textContent;")).contains("pribrendo"))) {
                    WebElement harvest = driver.findElement(By.cssSelector(".btn-green"));
                    harvest.click();
                    break;
                }
            }
        }

        //Sell
        {
            int money = Integer.parseInt(driver.findElement(By.cssSelector(".renew-money")).getText().replaceAll("[^0-9]", ""));
            if (RUBY_FARMING && money >= 300) {
                return;
            }

            Thread.sleep(2000);
            //open inventory
            driver.findElement(By.cssSelector(".icons-pistol")).click();
            Thread.sleep(500);
            //Open sell dialogue, hover over first
            driver.manage().window().setSize(new Dimension(340, 700));
            Thread.sleep(700);
            executor.executeScript("arguments[0].click();", driver.findElement(By.cssSelector(".sellMarijuana")));
            driver.manage().window().setSize(new Dimension(1366, 768));
            Thread.sleep(500);
            //sell it
            driver.findElement(By.cssSelector(".btn-green")).click();
        }

        //grow harvest sell cycle is done, restart the tick
    }

    //Skill while we're growing, because why not TODO: make sure it's compatible with the captcha also
    private void handleSkilling(final long timeUntilHarvest) {
        int health = Integer.parseInt(driver.findElement(By.cssSelector(".renew-hp")).getText().split(" / ")[0]);
        int energy = Integer.parseInt(driver.findElement(By.cssSelector(".renew-energy")).getText().split(" / ")[0]);
        int awake = Integer.parseInt(driver.findElement(By.cssSelector(".renew-awake")).getText().split(" / ")[0]);
        int money = Integer.parseInt(driver.findElement(By.cssSelector(".renew-money")).getText().replaceAll("[^0-9]", ""));
        String[] skills = new String[] {
            "#speed .btn-green",
            "#strength .btn-green",
            "#defense .btn-green"
        };
        boolean canSkill = health > 2 && energy >= 3 && awake > 5 && money >= 1000;
        while (canSkill) {
            //TODO: this wont work if you can't see the buildings, horrible
            driver.findElement(By.cssSelector("area:nth-child(2)")).click();
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                driver.findElement(By.cssSelector(skills[rng.nextInt(3)])).click();
            } catch (Exception e) {
                //handleSkilling(timeUntilHarvest - (System.nanoTime() / 1000000 - delay.getCurrentTime()));
            }
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            health = Integer.parseInt(driver.findElement(By.cssSelector(".renew-hp")).getText().split(" / ")[0]);
            energy = Integer.parseInt(driver.findElement(By.cssSelector(".renew-energy")).getText().split(" / ")[0]);
            awake = Integer.parseInt(driver.findElement(By.cssSelector(".renew-awake")).getText().split(" / ")[0]);
            money = Integer.parseInt(driver.findElement(By.cssSelector(".renew-money")).getText().replaceAll("[^0-9]", ""));
            canSkill = health > 2 && energy >= 3 && awake > 5 && money >= 1000;
        }
    }

    private void handleBrothel() throws InterruptedException, TesseractException {
        while (true) {

            if (driver.findElements(By.className("disabled-map")).size() != 0) {
                continue; //We are in the hospital, let's not do anything
            }

            //Open brothel
            driver.findElement(By.cssSelector("area:nth-child(7)")).click();

            //Wait for button
            Thread.sleep(3000);

            //Start work
            executor.executeScript("document.getElementsByClassName('btn btn-green sendHookers')[0].click()");

            Thread.sleep(3000);
            //get the time until finished
            String[] timeUntil = ((String) executor.executeScript("return document.getElementsByClassName('working left-time')[0].textContent;")).replaceAll("\n", "").replace(" ", "").split(":");
            while (timeUntil[0].contains("min")) {
                Thread.sleep(1000);
                timeUntil = ((String) executor.executeScript("return document.getElementsByClassName('working left-time')[0].textContent;")).replaceAll("\n", "").replace(" ", "").split(":");
            }
            long timeUntilMillis = (Long.parseLong(timeUntil[0]) * 3600000 + Long.parseLong(timeUntil[1]) * 60000 + Long.parseLong(timeUntil[2]) * 1000) + 10000;

            Timer freeTime = new Timer();

            //Wait for the work to end
            while (!freeTime.hasTimePassed(timeUntilMillis)) {
                /*                if(driver.findElements(By.className("disabled-map")).size() != 0){
                                    continue; //We are in the hospital, let's not do anything
                                }
                                handleSkilling(420);*/
                long timeLeft = timeUntilMillis - (System.nanoTime() / 1000000 - freeTime.getCurrentTime());
                String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeLeft),
                    TimeUnit.MILLISECONDS.toMinutes(timeLeft) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeLeft)),
                    TimeUnit.MILLISECONDS.toSeconds(timeLeft) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeLeft)));
                this.harvestStatus.setText("Until collection: " + hms);
                if (RUBY_FARMING) {
                    botTick();
                }
            }

            //Open brothel
            driver.findElement(By.cssSelector("area:nth-child(7)")).click();

            //Wait for button, collect money
            Thread.sleep(3000);
            executor.executeScript("document.getElementsByClassName('btn btn-green collectMoney pulseAnim')[0].click()");
        }
    }

    public BotDriver() {
        this.driver = new ChromeDriver();
        this.executor = (JavascriptExecutor) driver;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        setBounds(20, 50, 950, 500);

        getRootPane().putClientProperty("apple.awt.draggableWindowBackground", false);
        title.setForeground(new Color(0, 0, 0, 255));
        harvestStatus.setForeground(new Color(0, 0, 0, 255));
        title.setVisible(true);
        harvestStatus.setVisible(true);
        getContentPane().setLayout(
            new BoxLayout(getContentPane(), BoxLayout.Y_AXIS)
        );
        getContentPane().add(title);
        getContentPane().add(harvestStatus);
        setVisible(true);
        pack();
    }

}