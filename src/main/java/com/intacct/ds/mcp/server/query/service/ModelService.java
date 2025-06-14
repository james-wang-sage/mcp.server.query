package com.intacct.ds.mcp.server.query.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Service to interact with the Intacct Core Model API.
 * Provides functionality to retrieve model definitions for various resources.
 */
@Service
public class ModelService {

    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    private final RestClient restClient;
    private final AuthService authService;
    private String currentAccessToken; // Store the token used by this instance's RestClient
    private final String baseUrl; // Store the base URL for this instance

    @Autowired
    public ModelService(AuthService authService) {
        this.authService = authService;
        this.currentAccessToken = this.authService.getAccessToken();

        // Get base URL from AuthService's baseUrl (which already handles properties priority)
        // This ensures ModelService uses the same baseUrl as AuthService
        this.baseUrl = authService.getBaseUrl();

        if (this.currentAccessToken == null) {
            // Handle initialization failure - maybe throw an exception?
            logger.error("Failed to obtain access token during initialization. ModelService may not function correctly.");
            // Set up a non-functional RestClient or throw to prevent use
             this.restClient = RestClient.builder().build(); // Or throw new IllegalStateException(...)
             return;
        }

        // Configure RestClient with base URL and Authorization header
        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.currentAccessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "MCP-Query-Server/1.0 (Java)")
                .build();
    }

    // --- DTOs based on common.openapi.yaml ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelApiResponse(
            @JsonProperty("ia::result") ObjectModel result,
            @JsonProperty("ia::meta") Metadata meta
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObjectModel(
            Map<String, FieldDefinition> fields,
            Map<String, GroupDefinition> groups,
            Map<String, RefDefinition> refs,
            Object lists, // Changed to Object to handle cases where 'lists' is an object instead of an array
            Boolean idempotenceSupported,
            String httpMethods
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FieldDefinition(
            Boolean mutable,
            Boolean nullable,
            String type,
            String format, // e.g., date-time
            Boolean readOnly,
            Boolean writeOnly,
            Boolean required,
            @JsonProperty("enum") List<String> enumValues // Map from 'enum'
    ) {
        // Default constructor for Jackson
        @JsonCreator
        public FieldDefinition(
            @JsonProperty("mutable") Boolean mutable,
            @JsonProperty("nullable") Boolean nullable,
            @JsonProperty("type") String type,
            @JsonProperty("format") String format,
            @JsonProperty("readOnly") Boolean readOnly,
            @JsonProperty("writeOnly") Boolean writeOnly,
            @JsonProperty("required") Boolean required,
            @JsonProperty("enum") List<String> enumValues
        ) {
            this.mutable = mutable;
            this.nullable = nullable;
            this.type = type;
            this.format = format;
            this.readOnly = readOnly;
            this.writeOnly = writeOnly;
            this.required = required;
            this.enumValues = enumValues;
            // Default values can be set here if needed, e.g., if a field is sometimes absent
         }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroupDefinition(
            Map<String, FieldDefinition> fields
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefDefinition(
            String apiObject,
            Map<String, FieldDefinition> fields
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            Integer totalCount,
            Integer totalSuccess,
            Integer totalError
    ) {}

    // DTO for the response when listing all models (assuming result is a list of summaries)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelListApiResponse(
            @JsonProperty("ia::result") List<ResourceSummary> result,
            @JsonProperty("ia::meta") Metadata meta
    ) {}

    // DTO for a single resource summary in the list response
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResourceSummary(
            @JsonProperty("apiObject") // Found the correct field name from raw response
            String apiObject,
            String type,
            // Add other potential summary fields if known or needed
            String description,
            String version
            // httpMethods? // Depending on what the summary includes
    ) {}

    /**
     * Get an object model definition from the Intacct API.
     * Corresponds to the /services/core/model GET endpoint.
     *
     * @param name    Resource name (e.g., accounts-payable/vendor). Required.
     * @param type    Optional resource type (e.g., object, service).
     * @param version Optional API version (e.g., v1).
     * @param schema  Optional flag to include full schema ("true"/"false").
     * @param tags    Optional flag for schema formatting ("true"/"false").
     * @return The ObjectModel definition, or null if an error occurs.
     */
    @Tool(description = "Get an object model definition. Lists fields, relationships, etc. for an Intacct resource (object, service).")
    public ObjectModel getModelDefinition(
            @ToolParam(description = "Resource name (e.g., 'accounts-payable/vendor' or 'platform-apps/nsp::<object-name>')") String name,
            @ToolParam(description = "Filter by resource type (e.g., 'object', 'service')", required = false) String type,
            @ToolParam(description = "API version (e.g., 'v1', 'ALL')", required = false) String version,
            @ToolParam(description = "Include full model ('true'/'false' as string, defaults based on context)", required = false) String schema,
            @ToolParam(description = "Return schema without tags ('true'/'false' as string, default 'true')", required = false) String tags
    ) {
        Objects.requireNonNull(name, "Resource name cannot be null");

        // Fix common mistake: remove "objects/" prefix if present
        final String resourceName;
        if (name.startsWith("objects/")) {
            resourceName = name.substring("objects/".length());
            logger.debug("Removed 'objects/' prefix from resource name. Using: {}", resourceName);
        } else {
            resourceName = name;
        }

        // Check if RestClient was initialized properly (access token obtained)
        if (this.currentAccessToken == null) {
             logger.error("Cannot get model definition: Access token was not obtained during initialization.");
             return null; // Or throw an exception
        }

        // Use UriBuilder within the RestClient call to correctly combine with base URL
        logger.info("Requesting model definition for name: {}, type: {}, version: {}, schema: {}, tags: {}",
                resourceName, type, version, schema, tags);

        try {
            ModelApiResponse response = restClient.get()
                    .uri(builder -> {
                        builder = builder.path("/services/core/model").queryParam("name", resourceName);
                        // Add optional parameters if they are provided
                        if (type != null && !type.isEmpty()) {
                            builder.queryParam("type", type);
                        }
                        if (version != null && !version.isEmpty()) {
                            builder.queryParam("version", version);
                        }
                        if (schema != null && !schema.isEmpty()) {
                            builder.queryParam("schema", schema);
                        }
                        if (tags != null && !tags.isEmpty()) {
                            builder.queryParam("tags", tags);
                        }
                        URI builtUri = builder.build();
                        logger.debug("Constructed URI for request: {}", builtUri); // Log the final URI
                        return builtUri;
                    })
                    .retrieve()
                    .body(ModelApiResponse.class);

            if (response != null) {
                 logger.debug("Successfully retrieved model definition for '{}'", resourceName);
                 return response.result();
            } else {
                 logger.warn("Received null response for model definition request: {}", resourceName);
                 return null;
            }
        } catch (RestClientException e) {
            logger.error("Error retrieving model definition for '{}': {}", resourceName, e.getMessage(), e);
            // Consider throwing a custom exception or returning a specific error object
            // TODO: Handle potential 401 Unauthorized if token expires
            return null;
        }
    }

    /**
     * List all available object model definitions (summaries) from the Intacct API.
     * Corresponds to calling the /services/core/model GET endpoint without the 'name' parameter.
     *
     * @return A list of ResourceSummary objects, or null if an error occurs.
     */
    @Tool(description = "List all available Intacct resource model summaries (e.g., object names and types).")
    public List<ResourceSummary> listAvailableModels() {
        // Check if RestClient was initialized properly
        if (this.currentAccessToken == null) {
            logger.error("Cannot list models: Access token was not obtained during initialization.");
            return null; // Or throw?
        }

        logger.info("Requesting list of all available models...");
        logger.debug("Using base URL: {}", this.baseUrl);
        logger.debug("Current access token (first 10 chars): {}",
                this.currentAccessToken != null ? this.currentAccessToken.substring(0, Math.min(10, this.currentAccessToken.length())) + "..." : "null");

        try {
            // Log the full request URL
            String fullUrl = this.baseUrl + "/services/core/model";
            logger.debug("Making GET request to: {}", fullUrl);

            ModelListApiResponse response = restClient.get()
                    .uri("/services/core/model") // No query parameters for listing
                    .retrieve()
                    .body(ModelListApiResponse.class);

            if (response != null) {
                logger.debug("Raw response received: {}", response);

                if (response.result() != null) {
                    logger.info("Successfully retrieved list of {} models.", response.result().size());

                    // Log the first few models to help with debugging
                    if (!response.result().isEmpty()) {
                        int logLimit = Math.min(3, response.result().size());
                        logger.debug("First {} models: {}", logLimit,
                                response.result().subList(0, logLimit));
                    }

                    return response.result(); // Return the list
                } else {
                    logger.warn("Response was not null, but response.result() was null");
                    return List.of(); // Return empty list
                }
            } else {
                logger.warn("Received null response when listing models.");
                return List.of(); // Return empty list
            }
        } catch (RestClientException e) {
            logger.error("Error retrieving list of models: {}", e.getMessage(), e);
            logger.error("Exception details:", e); // Log the full stack trace

            // Try to get more details about the error
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }

            return null;
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            logger.error("Unexpected error in listAvailableModels: {}", e.getMessage(), e);
            return null;
        }
    }

    // Example main method for testing (optional)
    // mvn compile exec:java -Dexec.mainClass="com.intacct.ds.mcp.server.query.service.ModelService" -Dexec.classpathScope="test" -Dexec.args="accounts-payable/vendor" | cat
    // mvn compile exec:java -Dexec.mainClass="com.intacct.ds.mcp.server.query.service.ModelService" -Dexec.classpathScope="test" | cat
    public static void main(String[] args) {
        // For standalone testing, create AuthService manually
        AuthService authService = new AuthService();
        ModelService service = new ModelService(authService);

        // Exit if token acquisition failed during service construction
        if (service.restClient == null || service.currentAccessToken == null) {
             System.err.println("ModelService initialization failed due to token acquisition error. Exiting.");
             return;
        }

        if (args.length > 0) {
            String resourceName = args[0];
            System.out.println("Fetching detailed model for: " + resourceName);
            ObjectModel model = service.getModelDefinition(resourceName, null, null, "true", null);

            if (model != null) {
                System.out.println("Model definition for: " + resourceName);
                System.out.println("  Fields: " + model.fields().keySet());
                System.out.println("  Groups: " + (model.groups() != null ? model.groups().keySet() : "N/A"));
                System.out.println("  Refs: " + (model.refs() != null ? model.refs().keySet() : "N/A"));
                System.out.println("  Lists: " + model.lists()); // Print the raw 'lists' object
                System.out.println("  HTTP Methods: " + model.httpMethods());
                // Print more details as needed
            } else {
                System.out.println("Failed to retrieve model definition for: " + resourceName);
            }
        } else {
            System.out.println("No resource name provided. Fetching list of available models...");
            List<ResourceSummary> models = service.listAvailableModels(); // Call the correct method

            if (models != null) {
                System.out.println("Found " + models.size() + " available models:");
                models.forEach(summary ->
                        System.out.printf("  - API Object: %s, Type: %s%n", summary.apiObject(), summary.type())); // Corrected field name
            }
        }
    }
}
