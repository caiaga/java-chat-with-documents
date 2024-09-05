// Define the map for resolving specific pages
// this is needed because the page's relative path might not be complete in the embedded context.
const pages = {
    'best-practices': '/administration/best-practices',
    'resources': '/administration/best-practices/resources.md',
    'rotating-keys': '/administration/best-practices/rotating-keys.md',
    'scaling': '/administration/best-practices/scaling.md',
    'custom-themes': '/customization/custom-themes.md',
    'custom-user-attributes': '/customization/custom-user-attributes.md',
    'deploy-custom-theme': '/customization/deploy-custom-theme.md',
    'oidc-customization-considerations': '/customization/oidc-customization-considerations.md',
    'helm': '/deployment/helm',
    'additional-configuration-details': '/deployment/additional-configuration-details.md',
    'configuration-properties': '/deployment/configuration-properties.md',
    'install': '/deployment/install.md',
    'troubleshooting': '/deployment/troubleshooting.md',
    'uninstall': '/deployment/uninstall.md',
    'using-postgres-ha': '/deployment/using-postgres-ha.md',
    'configuration': '/deployment/configuration.md',
    'docker-compose': '/deployment/docker-compose.md',
    'docker': '/deployment/docker.md',
    'oidc': '/getting-started/oidc',
    'jwt-tokens': '/getting-started/oidc/jwt-tokens.md',
    'service-overview': '/getting-started/service-overview',
    'system-requirments': '/getting-started/system-requirments',
    'terms-abbreviations': '/getting-started/terms-abbreviations.md',
    'ds-integration': '/integration/ds-integration',
    'cnx': '/integration/ds-integration/cnx',
    'cnx-integration': '/integration/ds-integration/cnx/cnx-integration.md',
    'cnx-keycloak-configuration': '/integration/ds-integration/cnx/cnx-keycloak-configuration.md',
    'dx': '/integration/ds-integration/dx',
    'automation': '/integration/ds-integration/dx/automation',
    'dx-oidc-automation': '/integration/ds-integration/dx/automation/dx-oidc-automation.md',
    'transient-users': '/integration/ds-integration/dx/transient-users',
    'dx-update-webshpere-for-oidc-transient-users': '/integration/ds-integration/dx/transient-users/dx-update-webshpere-for-oidc-transient-users.md',
    'transient-users-building-jaas-modules': '/integration/ds-integration/dx/transient-users/transient-users-building-jaas-modules.md',
    'transient-users-softgroups-configuration': '/integration/ds-integration/dx/transient-users/transient-users-softgroups-configuration.md',
    'dx-integration': '/integration/ds-integration/dx/dx-integration.md',
    'dx-keycloak-configuration': '/integration/ds-integration/dx/dx-keycloak-configuration.md',
    'dx-oidc-customization-considerations': '/integration/ds-integration/dx/dx-oidc-customization-considerations.md',
    'dx-update-webshpere-for-oidc': '/integration/ds-integration/dx/dx-update-webshpere-for-oidc.md',
    'oidc-troubleshooting': '/integration/ds-integration/dx/oidc-troubleshooting.md',
    'idp-integration': '/integration/idp-integration',
    'azure-oidc-integration': '/integration/idp-integration/azure-oidc-integration.md',
    'azure-saml-integration': '/integration/idp-integration/azure-saml-integration.md',
}

// Function to strip '.md' from a URL or string
function stripMdExtension(url) {
    return url.replace('.md', '');
}

// Function to correct the URL based on the relative path or map lookup
function correctUrl(originalUrl) {
    // Define the base URL for the correct links (the GitHub repository in your case)
    const baseUrl = 'https://github.com/HCL-TECH-SOFTWARE/hclds-keycloak/tree/main/docs';

    // Strip leading './' or '/' from the original URL if it exists
    let relativePath = originalUrl.replace(/^\.\//, '').replace(/^\//, '');

    // Strip '.md' extension
    let cleanedPath = stripMdExtension(relativePath);

    // Check if the path exists in the pages map
    if (pages[cleanedPath]) {
        // If found, use the mapped path from the pages object
        return `${baseUrl}${pages[cleanedPath]}`;
    }

    // If not found in the map, construct the full URL using the default logic
    return `${baseUrl}/${relativePath}`;
}

// Function to open the corrected URL in a new tab
function openCorrectedUrl(event, originalUrl) {
    event.preventDefault(); // Prevent the default link behavior
    let correctedUrl = correctUrl(originalUrl);
    window.open(correctedUrl, '_blank'); // Open the corrected URL in a new tab
}

// Mutation Observer to listen for new links added to the page
const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
        // Loop through all added nodes
        mutation.addedNodes.forEach((node) => {
            if (node.nodeType === 1) { // Check if the added node is an element
                // Find all <a> elements within the added node
                let links = node.querySelectorAll('a');

                links.forEach((link) => {
                    let originalHref = link.getAttribute('href');
                    if (originalHref && originalHref.startsWith('./')) {
                        // If the href starts with './', we know it's a relative URL

                        // You can either update the href directly:
                        let correctedHref = correctUrl(originalHref);
                        link.setAttribute('href', correctedHref);

                        // OR you can add an onclick handler to handle the link opening logic
                        link.onclick = (event) => openCorrectedUrl(event, originalHref);
                    }
                });
            }
        });
    });
});

// Start observing the document for changes
observer.observe(document.body, { childList: true, subtree: true });
