$(document).ready(function () {
    if (window.MCPGateway.readAuth()) {
        window.location.href = "/admin.html";
        return;
    }

    const $error = $("#accessError");
    const $errorText = $("#accessError span");

    function showError(message) {
        $errorText.text(message);
        $error.removeClass("d-none").addClass("d-flex");
    }

    function hideError() {
        $error.addClass("d-none").removeClass("d-flex");
    }

    function saveAndEnter(tokenData, label) {
        window.MCPGateway.saveAuth({
            token: tokenData.accessToken || tokenData.token,
            profile: tokenData.profile,
            environment: tokenData.environment || window.MCPGateway.config.defaultEnvironment,
            scopes: tokenData.scopes || [],
            managedSystems: tokenData.managedSystems || [],
            expiresAt: tokenData.expiresAt,
            label: label || tokenData.profile
        });
        window.location.href = "/admin.html";
    }

    $("#enterDemoAdminBtn").on("click", async function () {
        hideError();
        const $btn = $(this);
        const originalHtml = $btn.html();
        $btn.prop("disabled", true).html('<span class="spinner-border spinner-border-sm me-2" role="status"></span>进入中...');
        try {
            const tokenData = await window.MCPGateway.issueDemoToken("demo-admin");
            saveAndEnter(tokenData, "Demo Admin");
        } catch (error) {
            showError(error.message);
        } finally {
            $btn.prop("disabled", false).html(originalHtml);
        }
    });

    $("#enterDemoAppBtn").on("click", async function () {
        hideError();
        const $btn = $(this);
        const originalHtml = $btn.html();
        $btn.prop("disabled", true).html('<span class="spinner-border spinner-border-sm me-2" role="status"></span>进入中...');
        try {
            const tokenData = await window.MCPGateway.issueDemoToken("demo-app");
            saveAndEnter(tokenData, "Demo App");
        } catch (error) {
            showError(error.message);
        } finally {
            $btn.prop("disabled", false).html(originalHtml);
        }
    });

    $("#customTokenForm").on("submit", async function (event) {
        event.preventDefault();
        hideError();

        const token = $("#customBearerToken").val().trim();
        if (!token) {
            showError("请先输入 Bearer Token");
            return;
        }

        const $btn = $(this).find("button[type='submit']");
        const originalHtml = $btn.html();
        $btn.prop("disabled", true).html('<span class="spinner-border spinner-border-sm me-2" role="status"></span>校验中...');

        try {
            const session = await window.MCPGateway.validateBearerToken(token);
            saveAndEnter(
                {
                    token: token,
                    profile: session.profile,
                    environment: session.environment,
                    scopes: session.scopes,
                    managedSystems: session.managedSystems,
                    expiresAt: session.expiresAt
                },
                session.profile
            );
        } catch (error) {
            showError(error.message);
        } finally {
            $btn.prop("disabled", false).html(originalHtml);
        }
    });

    $("#customBearerToken").on("input", hideError);
});
