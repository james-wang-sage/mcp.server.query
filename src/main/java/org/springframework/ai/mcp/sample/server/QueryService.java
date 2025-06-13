package org.springframework.ai.mcp.sample.server;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Service to interact with the Intacct Core Query API.
 * Provides functionality to query data in a Sage Intacct company.
 */
@Service
public class QueryService {

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    private final RestClient restClient;
    private final AuthService authService;
    private String currentAccessToken; // Store the token used by this instance's RestClient
    private final String baseUrl; // Store the base URL for this instance

    @Autowired
    public QueryService(AuthService authService) {
        this.authService = authService;
        this.currentAccessToken = this.authService.getAccessToken();

        // Get base URL from AuthService's baseUrl (which already handles properties priority)
        // This ensures QueryService uses the same baseUrl as AuthService
        this.baseUrl = authService.getBaseUrl();

        if (this.currentAccessToken == null) {
            logger.error("Failed to obtain access token during initialization. QueryService may not function correctly.");
            this.restClient = RestClient.builder().build();
            return;
        }

        // Configure RestClient with base URL and Authorization header
        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.currentAccessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // --- DTOs for Core Query API ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL) // Omit null fields from JSON request
    public record CoreQueryRequest(
            @JsonProperty("object") String object,
            @JsonProperty("fields") List<String> fields,
            // Using a flexible Map for filters due to the variety of operators ($eq, $gt, etc.)
            @JsonProperty("filters") List<Map<String, Map<String, Object>>> filters,
            @JsonProperty("filterExpression") String filterExpression,
            @JsonProperty("filterParameters") FilterParameters filterParameters,
            // Using a flexible Map for orderBy (e.g., [{"fieldName": "asc"}, {"otherField": "desc"}])
            @JsonProperty("orderBy") List<Map<String, String>> orderBy,
            @JsonProperty("start") Integer start,
            @JsonProperty("size") Integer size
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FilterParameters(
            String asOfDate,
            Boolean includeHierarchyFields,
            Boolean caseSensitiveComparison,
            Boolean includePrivate
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QueryApiResponse(
            @JsonProperty("ia::result") List<Map<String, Object>> result, // Results are dynamic maps
            @JsonProperty("ia::meta") MetadataPages meta
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetadataPages(
            Integer totalCount,
            Integer start,
            Integer pageSize,
            Integer next,
            Integer previous
    ) {}


    /**
     * Executes a query against a specified Sage Intacct object.
     * Corresponds to the /services/core/query POST endpoint.
     *
     * @param object           Object type to query (e.g., "accounts-payable/vendor"). Required.
     * @param fields           List of fields to include in the response (e.g., ["id", "name", "status"]). Optional.
     * @param filters          List of filter conditions. Required if filterExpression is used. Each condition must be a Map like: {"operator": {"field": "value"}}. Example: `[{"$eq": {"status": "active"}}]`
     *                       
     *                       Supported filter operators (with JSON examples):
     *                       - $eq: Equal to
     *                         Example: {"$eq": {"status": "active"}}
     *                       - $ne: Not equal to
     *                         Example: {"$ne": {"status": "inactive"}}
     *                       - $lt: Less than
     *                         Example: {"$lt": {"totalDue": 100}}
     *                       - $lte: Less than or equal to
     *                         Example: {"$lte": {"totalDue": 500}}
     *                       - $gt: Greater than
     *                         Example: {"$gt": {"totalDue": 1000}}
     *                       - $gte: Greater than or equal to
     *                         Example: {"$gte": {"totalDue": 999}}
     *                       - $in: In list of values
     *                         Example: {"$in": {"firstName": ["Tim", "Anjali", "Gabriel"]}}
     *                       - $notIn: Not in list of values
     *                         Example: {"$notIn": {"country": ["United States", "Canada"]}}
     *                       - $between: Between two values (inclusive)
     *                         Example: {"$between": {"totalDue": [1, 1000]}}
     *                       - $notBetween: Not between two values
     *                         Example: {"$notBetween": {"totalDue": [100, 1000]}}
     *                       - $contains: Contains substring/number (not for dates)
     *                         Example: {"$contains": {"name": "Acme"}}
     *                       - $notContains: Does not contain substring/number (not for dates)
     *                         Example: {"$notContains": {"name": "llc"}}
     *                       - $startsWith: Starts with substring/number (not for dates)
     *                         Example: {"$startsWith": {"name": "A"}}
     *                       - $notStartsWith: Does not start with substring/number (not for dates)
     *                         Example: {"$notStartsWith": {"name": "Q"}}
     *                       - $endsWith: Ends with substring/number (not for dates)
     *                         Example: {"$endsWith": {"name": "inc"}}
     *                       - $notEndsWith: Does not end with substring/number (not for dates)
     *                         Example: {"$notEndsWith": {"name": "llc"}}
     *                       
     *                       For date fields, you can use macro values like 'today', 'yesterday', 'currentMonth', etc. Example: {"$eq": {"postingDate": "priorYear"}}
     * @param filterExpression Logical operators for multiple filters (e.g., "1 and 2"). Defaults to "and". Optional.
     *                        
     *                        How to use filterExpression:
     *                        - Use when you have multiple filters and need to combine them with custom logic.
     *                        - The value references the 1-based index of each filter in the 'filters' array.
     *                        - Supported operators: and, or, parentheses for grouping.
     *                        - Examples:
     *                          "1 and 2" (both conditions must be true)
     *                          "1 or 2" (either condition is true)
     *                          "(1 and 2) or 3" (grouping)
     *                        - If omitted, all filters are combined with AND by default.
     *                        - If only one filter is present, omit this parameter entirely.
     *                        - Do NOT put field conditions like 'status = active' here; only use filter indices and logical operators.
     *
     * @param filterParameters Additional filter options (asOfDate, caseSensitiveComparison, etc.). Optional.
     *                        
     *                        Supported filterParameters fields:
     *                        - asOfDate (String, format: yyyy-MM-dd):
     *                          The reference date for relative date macros in filters. Example: "2022-04-01"
     *                        - includeHierarchyFields (Boolean):
     *                          If true, includes hierarchy structure info in each object. Default: false
     *                        - caseSensitiveComparison (Boolean):
     *                          If false, query is case-insensitive. Default: true
     *                        - includePrivate (Boolean):
     *                          If true, includes data from private entities in multi-entity companies. Default: false
     *                        
     *                        Example:
     *                        {
     *                          "asOfDate": "2022-04-01",
     *                          "includeHierarchyFields": false,
     *                          "caseSensitiveComparison": true,
     *                          "includePrivate": true
     *                        }
     * @param orderBy          Sort order for results (e.g., [{"id": "asc"}]). Optional.
     * @param start            First record to include (for pagination). Optional.
     * @param size             Number of records to include (page size). Optional.
     * @return A list of maps, where each map represents a result object with its requested fields, or null on error.
     */
    @Tool(description = "Query data from a Sage Intacct object using filters, field selection, ordering, and pagination.\n\n" +
            "How to use filters:\n" +
            "- Use the 'filters' array to specify filter conditions (e.g., {\"$eq\": {\"status\": \"active\"}}).\n" +
            "- Each filter is a map: {\"operator\": {\"field\": value}}.\n" +
            "- Supported filter operators (with JSON examples):\n" +
            "  - $eq: Equal to\n" +
            "    Example: {\"$eq\": {\"status\": \"active\"}}\n" +
            "  - $ne: Not equal to\n" +
            "    Example: {\"$ne\": {\"status\": \"inactive\"}}\n" +
            "  - $lt: Less than\n" +
            "    Example: {\"$lt\": {\"totalDue\": 100}}\n" +
            "  - $lte: Less than or equal to\n" +
            "    Example: {\"$lte\": {\"totalDue\": 500}}\n" +
            "  - $gt: Greater than\n" +
            "    Example: {\"$gt\": {\"totalDue\": 1000}}\n" +
            "  - $gte: Greater than or equal to\n" +
            "    Example: {\"$gte\": {\"totalDue\": 999}}\n" +
            "  - $in: In list of values\n" +
            "    Example: {\"$in\": {\"firstName\": [\"Tim\", \"Anjali\", \"Gabriel\"]}}\n" +
            "  - $notIn: Not in list of values\n" +
            "    Example: {\"$notIn\": {\"country\": [\"United States\", \"Canada\"]}}\n" +
            "  - $between: Between two values (inclusive)\n" +
            "    Example: {\"$between\": {\"totalDue\": [1, 1000]}}\n" +
            "  - $notBetween: Not between two values\n" +
            "    Example: {\"$notBetween\": {\"totalDue\": [100, 1000]}}\n" +
            "  - $contains: Contains substring/number (not for dates)\n" +
            "    Example: {\"$contains\": {\"name\": \"Acme\"}}\n" +
            "  - $notContains: Does not contain substring/number (not for dates)\n" +
            "    Example: {\"$notContains\": {\"name\": \"llc\"}}\n" +
            "  - $startsWith: Starts with substring/number (not for dates)\n" +
            "    Example: {\"$startsWith\": {\"name\": \"A\"}}\n" +
            "  - $notStartsWith: Does not start with substring/number (not for dates)\n" +
            "    Example: {\"$notStartsWith\": {\"name\": \"Q\"}}\n" +
            "  - $endsWith: Ends with substring/number (not for dates)\n" +
            "    Example: {\"$endsWith\": {\"name\": \"inc\"}}\n" +
            "  - $notEndsWith: Does not end with substring/number (not for dates)\n" +
            "    Example: {\"$notEndsWith\": {\"name\": \"llc\"}}\n" +
            "- For date fields, you can use macro values like 'today', 'yesterday', 'currentMonth', etc. Example: {\"$eq\": {\"postingDate\": \"priorYear\"}}\n" +
            "\n" +
            "How to use filterExpression:\n" +
            "- Use when you have multiple filters and need to combine them with custom logic.\n" +
            "- The value references the 1-based index of each filter in the 'filters' array.\n" +
            "- Supported operators: and, or, parentheses for grouping.\n" +
            "- Examples:\n" +
            "  '1 and 2' (both conditions must be true)\n" +
            "  '1 or 2' (either condition is true)\n" +
            "  '(1 and 2) or 3' (grouping)\n" +
            "- If omitted, all filters are combined with AND by default.\n" +
            "- If only one filter is present, omit this parameter entirely.\n" +
            "- Do NOT put field conditions like 'status = active' here; only use filter indices and logical operators.\n" +
            "\n" +
            "How to use filterParameters:\n" +
            "- asOfDate (String, format: yyyy-MM-dd): The reference date for relative date macros in filters. Example: '2022-04-01'\n" +
            "- includeHierarchyFields (Boolean): If true, includes hierarchy structure info in each object. Default: false\n" +
            "- caseSensitiveComparison (Boolean): If false, query is case-insensitive. Default: true\n" +
            "- includePrivate (Boolean): If true, includes data from private entities in multi-entity companies. Default: false\n" +
            "- Example:\n" +
            "  {\n" +
            "    'asOfDate': '2022-04-01',\n" +
            "    'includeHierarchyFields': false,\n" +
            "    'caseSensitiveComparison': true,\n" +
            "    'includePrivate': true\n" +
            "  }\n" +
            "\n" +
            "How to use filters and filterExpression together:\n" +
            "- Example 1: Single filter (filterExpression is omitted):\n" +
            "  {\n" +
            "    'object': 'accounts-payable/vendor',\n" +
            "    'fields': ['id', 'name', 'status'],\n" +
            "    'filters': [ { '$eq': { 'status': 'active' } } ],\n" +
            "    'orderBy': [ { 'id': 'asc' } ],\n" +
            "    'size': 3\n" +
            "  }\n" +
            "- Example 2: Multiple filters combined with AND (filterExpression omitted, default behavior):\n" +
            "  {\n" +
            "    'object': 'accounts-payable/vendor',\n" +
            "    'fields': ['id', 'name', 'status', 'billingType'],\n" +
            "    'filters': [ { '$eq': { 'status': 'active' } }, { '$eq': { 'billingType': 'openItem' } } ],\n" +
            "    'orderBy': [ { 'id': 'asc' } ],\n" +
            "    'size': 10\n" +
            "  }\n" +
            "- Example 3: Multiple filters combined with specific logic (using filterExpression):\n" +
            "  {\n" +
            "    'object': 'accounts-payable/vendor',\n" +
            "    'fields': ['id', 'name', 'status', 'totalDue'],\n" +
            "    'filters': [ { '$eq': { 'status': 'active' } }, { '$gt': { 'totalDue': 1000 } }, { '$eq': { 'country': 'USA' } } ],\n" +
            "    'filterExpression': '(1 or 2) and 3',\n" +
            "    'orderBy': [ { 'id': 'asc' } ],\n" +
            "    'size': 20\n" +
            "  }\n" )
    public List<Map<String, Object>> executeQuery(
            @ToolParam(description = "Object type to query (e.g., 'accounts-payable/vendor'). Required.") String object,
            @ToolParam(description = "List of fields to include (e.g., [\"id\", \"name\"]).") List<String> fields,
            @ToolParam(description = "List of filter conditions. Required if filterExpression is used. Each is a Map: {\"operator\": {\"field\": \"value\"}}. Ex: `[{\"$eq\": {\"status\": \"active\"}}]`", required = false) List<Map<String, Map<String, Object>>> filters,
            @ToolParam(description = "Combines filters from the 'filters' list using their 1-based index (e.g., \"1\", \"1 and 2\"). Do NOT put conditions like 'status = active' here.", required = false) String filterExpression,
            @ToolParam(description = "Additional filter options (asOfDate, caseSensitive, etc.).", required = false) FilterParameters filterParameters,
            @ToolParam(description = "Sort order (e.g., [{\"id\": \"asc\"}]).", required = false) List<Map<String, String>> orderBy,
            @ToolParam(description = "Starting record number (for pagination).", required = false) Integer start,
            @ToolParam(description = "Page size (number of records to return).", required = false) Integer size
    ) {
        Objects.requireNonNull(object, "Query object cannot be null");

        // Fix common mistake: remove "objects/" prefix if present
        if (object.startsWith("objects/")) {
            object = object.substring("objects/".length());
            logger.debug("Removed 'objects/' prefix from object name. Using: {}", object);
        }

        // Validate that filterExpression is only used when filters are provided
        if (filterExpression != null && !filterExpression.trim().isEmpty()
                && (filters == null || filters.isEmpty())) {
            throw new IllegalArgumentException(
                    "The 'filterExpression' parameter requires at least one entry in 'filters'.");
        }

        // Normalize blank filterExpression to null so the API can apply its default behaviour
        if (filterExpression != null && filterExpression.trim().isEmpty()) {
            filterExpression = null;
        }

        if (this.currentAccessToken == null) {
            logger.error("Cannot execute query: Access token was not obtained during initialization.");
            return null;
        }

        CoreQueryRequest requestBody = new CoreQueryRequest(
                object,
                fields,
                filters,
                filterExpression, // API defaults to 'and' when filterExpression is null
                filterParameters,
                orderBy,
                start,
                size
        );

        logger.info("Executing query for object: {}", object);
        logger.debug("Query Request Body: {}", requestBody); // Be cautious logging request bodies if they contain sensitive data

        try {
            QueryApiResponse response = restClient.post()
                    .uri("/services/core/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(QueryApiResponse.class);

            if (response != null && response.result() != null) {
                logger.info("Successfully executed query for object '{}', received {} results.", object, response.result().size());
                logger.debug("Query metadata: {}", response.meta());
                return response.result();
            } else {
                logger.warn("Received null or empty result list for query on object: {}", object);
                return List.of(); // Return empty list
            }
        } catch (RestClientException e) {
            logger.error("Error executing query for object '{}': {}", object, e.getMessage(), e);
            // Consider parsing the error response body for more details if possible
            return null;
        }
    }

    // Example main method for testing
     public static void main(String[] args) {
         // For standalone testing, create AuthService manually
         AuthService authService = new AuthService();
         QueryService service = new QueryService(authService);

         // Exit if token acquisition failed during service construction
         if (service.restClient == null || service.currentAccessToken == null) {
            System.err.println("QueryService initialization failed due to token acquisition error. Exiting.");
            return;
         }

         // Example 1: Simple query for active vendors
         System.out.println("--- Query 1: Active Vendors ---");
         List<Map<String, Object>> activeVendors = service.executeQuery(
                 "accounts-payable/vendor",
                 List.of("id", "name", "status"),
                 List.of(Map.of("$eq", Map.of("status", "active"))), // Filter: status == active
                 null, // default filterExpression ('and')
                 null, // no filterParameters
                 List.of(Map.of("id", "asc")), // orderBy id ascending
                 null, // default start
                 5 // limit size to 5
         );

         if (activeVendors != null) {
             System.out.println("Found " + activeVendors.size() + " active vendors (first 5):");
             activeVendors.forEach(vendor -> System.out.println("  " + vendor));
         } else {
             System.out.println("Query for active vendors failed.");
         }

         // Example 2: Query departments (no filters, different fields)
         System.out.println("\n--- Query 2: Departments ---");
         List<Map<String, Object>> departments = service.executeQuery(
                 "company-config/department",
                 List.of("key", "id", "name", "status", "parent.id"), // Include parent ID
                 null, // no filters
                 null, // no filterExpression
                 null, // no filterParameters
                 List.of(Map.of("name", "asc")), // orderBy name ascending
                 null, // default start
                 null // default size
         );

         if (departments != null) {
             System.out.println("Found " + departments.size() + " departments:");
             departments.forEach(dept -> System.out.println("  " + dept));
         } else {
             System.out.println("Query for departments failed.");
         }
     }

} 
