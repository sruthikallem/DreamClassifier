package com.dreamportal.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.*;

import java.net.URI;
import java.net.http.*;
import java.util.List;
import org.json.*;

public class DreamClassificationTest {

    WebDriver driver;
    String openAIKey = "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";

    @BeforeMethod
    public void setup() {
        System.setProperty("webdriver.chrome.driver", "drivers/chromedriver.exe");
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get("https://arjitnigam.github.io/myDreams/dreams-diary.html");
    }

    @Test
    public void classifyDreams() throws Exception {
        Thread.sleep(4000); // wait for table to load

        List<WebElement> rows = driver.findElements(By.xpath("//table[@id='dreamsDiary']/tbody/tr"));
        System.out.println("✅ Number of dreams found: " + rows.size());

        for (WebElement row : rows) {
            String dreamName = row.findElement(By.xpath(".//td[1]")).getText().trim();
            String expected = row.findElement(By.xpath(".//td[3]")).getText().trim();

            System.out.println("\n📝 Dream: '" + dreamName + "' | Expected: '" + expected + "'");

            String aiResult = getDreamClassificationFromAI(dreamName);
            String aiClean = aiResult.trim().toLowerCase().replace(".", "");

            System.out.println("🤖 AI Classification: '" + aiClean + "'");

            // Defensive assertion: exact Good/Bad matching
            Assert.assertTrue(aiClean.contains(expected.toLowerCase()),
                    "❌ Mismatch for dream: '" + dreamName + "' | AI said: '" + aiResult + "' | Expected: '" + expected + "'");
        }

        System.out.println("\n✅ All dreams classified successfully.\n");
    }

    public String getDreamClassificationFromAI(String dream) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String prompt = "Classify this dream as Good or Bad: \"" + dream + "\". Just answer with Good or Bad.";

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-3.5-turbo");

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);

        requestBody.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAIKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.out.println("❌ API call failed with status: " + response.statusCode());
            System.out.println("Response Body: " + response.body());
            throw new RuntimeException("OpenAI API call failed.");
        }

        JSONObject json = new JSONObject(response.body());
        String reply = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        return reply.trim();
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
