package org.springframework.ai.mcp.sample.server;

/**
 * Simple test class to debug the ModelService.listAvailableModels method
 */
public class ModelServiceTest {

    public static void main(String[] args) {
        System.out.println("Starting ModelService test...");
        
        try {
            // Create the services manually
            AuthService authService = new AuthService();
            ModelService modelService = new ModelService(authService);
            
            // Test the listAvailableModels method
            System.out.println("Calling listAvailableModels...");
            var models = modelService.listAvailableModels();
            
            // Check the result
            if (models == null) {
                System.out.println("Result: null (error occurred)");
            } else if (models.isEmpty()) {
                System.out.println("Result: empty list (no models found)");
            } else {
                System.out.println("Result: " + models.size() + " models found");
                System.out.println("First few models:");
                models.stream().limit(5).forEach(model -> 
                    System.out.println("  - " + model)
                );
            }
        } catch (Exception e) {
            System.out.println("Exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("Test completed.");
    }
}
