package com.example.bajajassignment;

import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WebhookTrigger implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        

        RestTemplate restTemplate = new RestTemplate();

        String registrationUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        
        String name = "Aditya Sharma";
        String regNo = "2210992588";
        String email = "aditya2588.be22@chitkara.edu.in";

        
        HttpHeaders regHeaders = new HttpHeaders();
        regHeaders.setContentType(MediaType.APPLICATION_JSON);

        String registrationBody = String.format("""
            {
                "name": "%s",
                "regNo": "%s",
                "email": "%s"
            }
            """, name, regNo, email);

        HttpEntity<String> regEntity = new HttpEntity<>(registrationBody, regHeaders);

        
        ResponseEntity<String> regResponse = restTemplate.exchange(
                registrationUrl, HttpMethod.POST, regEntity, String.class);

        if (!regResponse.getStatusCode().is2xxSuccessful()) {
            System.err.println("Registration failed: " + regResponse.getStatusCode());
            return;
        }

        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(regResponse.getBody());

        String webhookUrl = rootNode.path("webhook").asText();
        String accessToken = rootNode.path("accessToken").asText();

        System.out.println("Webhook URL: " + webhookUrl);
        System.out.println("Access Token: " + accessToken);

        

        
        String digitsOnly = regNo.replaceAll("\\D+", ""); 
        String lastTwoDigitsStr;
        if (digitsOnly.length() >= 2) {
            lastTwoDigitsStr = digitsOnly.substring(digitsOnly.length() - 2);
        } else if (digitsOnly.length() == 1) {
            lastTwoDigitsStr = digitsOnly;
        } else {
            System.err.println("Invalid regNo format for last two digits extraction.");
            return;
        }

        int lastTwoDigits;
        try {
            lastTwoDigits = Integer.parseInt(lastTwoDigitsStr);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing last two digits of regNo: " + e.getMessage());
            return;
        }

        boolean isOdd = (lastTwoDigits % 2 == 1);

        String sqlQuery;

        if (isOdd) {
            
            sqlQuery = "SELECT p.amount AS SALARY, CONCAT(e.first_name, ' ', e.last_name) AS NAME, FLOOR(DATEDIFF(CURDATE(), e.dob) / 365) AS AGE, d.department_name AS DEPARTMENT_NAME FROM payments p JOIN employee e ON p.emp_id = e.emp_id JOIN department d ON e.department = d.department_id WHERE DAY(p.payment_time) != 1 ORDER BY p.amount DESC LIMIT 1";
        } else {
            
            sqlQuery = "SELECT e1.emp_id AS EMP_ID, e1.first_name AS FIRST_NAME, e1.last_name AS LAST_NAME, d.department_name AS DEPARTMENT_NAME, COUNT(e2.emp_id) AS YOUNGER_EMPLOYEES_COUNT FROM employee e1 JOIN department d ON e1.department = d.department_id LEFT JOIN employee e2 ON e1.department = e2.department AND e2.dob > e1.dob GROUP BY e1.emp_id, e1.first_name, e1.last_name, d.department_name ORDER BY e1.emp_id DESC";
        }

        System.out.println("Using SQL Query:");
        System.out.println(sqlQuery);

        
        HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);
        submitHeaders.setBearerAuth(accessToken);

        String submitBody = "{\"finalQuery\": \"" + sqlQuery.replace("\"", "\\\"") + "\"}";

        HttpEntity<String> submitEntity = new HttpEntity<>(submitBody, submitHeaders);

        ResponseEntity<String> submitResponse = restTemplate.exchange(
                webhookUrl, HttpMethod.POST, submitEntity, String.class);

        System.out.println("Submission response status: " + submitResponse.getStatusCode());
        System.out.println("Submission response body: " + submitResponse.getBody());
    }
}
