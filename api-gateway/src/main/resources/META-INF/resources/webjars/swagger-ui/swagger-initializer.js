window.onload = function () {
    window.ui = SwaggerUIBundle({
        // Multiple services - user can select from dropdown
        urls: [
            { url: "/v3/api-docs", name: "API Gateway" },
            { url: "/api/v1/auth/v3/api-docs", name: "Auth Service" },
            { url: "/api/v1/products/v3/api-docs", name: "Product Service" },
            { url: "/api/v1/orders/v3/api-docs", name: "Order Service" },
            { url: "/api/v1/users/v3/api-docs", name: "User Service" }
        ],
        "urls.primaryName": "Auth Service", // Default selection
        dom_id: '#swagger-ui',
        deepLinking: true,
        presets: [
            SwaggerUIBundle.presets.apis,
            SwaggerUIStandalonePreset
        ],
        plugins: [
            SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: "StandaloneLayout",
        validatorUrl: null,
        supportedSubmitMethods: ['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace']
    });
};
