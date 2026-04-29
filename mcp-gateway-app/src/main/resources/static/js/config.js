// js/config.js

const API_BASE_URL = `${window.location.origin}/api-gateway`;

const API_ENDPOINTS = {
    GET_GATEWAY_LIST: `${API_BASE_URL}/admin/query_gateway_config_list`,
    GET_GATEWAY_PAGE: `${API_BASE_URL}/admin/query_gateway_config_page`,
    SAVE_GATEWAY_CONFIG: `${API_BASE_URL}/admin/save_gateway_config`,
    SAVE_GATEWAY_TOOL_CONFIG: `${API_BASE_URL}/admin/save_gateway_tool_config`,
    GET_GATEWAY_PROTOCOL_LIST: `${API_BASE_URL}/admin/query_gateway_protocol_list`,
    GET_GATEWAY_PROTOCOL_PAGE: `${API_BASE_URL}/admin/query_gateway_protocol_page`,
    GET_GATEWAY_PROTOCOL_LIST_BY_ID: `${API_BASE_URL}/admin/query_gateway_protocol_list_by_gateway_id`,
    SAVE_GATEWAY_PROTOCOL: `${API_BASE_URL}/admin/save_gateway_protocol`,
    IMPORT_GATEWAY_PROTOCOL: `${API_BASE_URL}/admin/import_gateway_protocol`,
    ANALYSIS_PROTOCOL: `${API_BASE_URL}/admin/analysis_protocol`,
    DELETE_GATEWAY_PROTOCOL: `${API_BASE_URL}/admin/delete_gateway_protocol`,
    GET_GATEWAY_AUTH_LIST: `${API_BASE_URL}/admin/query_gateway_auth_list`,
    GET_GATEWAY_AUTH_PAGE: `${API_BASE_URL}/admin/query_gateway_auth_page`,
    GET_GATEWAY_AUTH_LIST_BY_ID: `${API_BASE_URL}/admin/query_gateway_auth_list_by_gateway_id`,
    SAVE_GATEWAY_AUTH: `${API_BASE_URL}/admin/save_gateway_auth`,
    DELETE_GATEWAY_AUTH: `${API_BASE_URL}/admin/delete_gateway_auth`,
    GET_GATEWAY_TOOL_LIST: `${API_BASE_URL}/admin/query_gateway_tool_list`,
    GET_GATEWAY_TOOL_PAGE: `${API_BASE_URL}/admin/query_gateway_tool_page`,
    GET_GATEWAY_TOOL_LIST_BY_ID: `${API_BASE_URL}/admin/query_gateway_tool_list_by_gateway_id`,
    DELETE_GATEWAY_TOOL: `${API_BASE_URL}/admin/delete_gateway_tool_config`,
    TEST_CALL_GATEWAY: `${API_BASE_URL}/admin/test_call_gateway`
};

const MOCK_ACCOUNT = {
    username: "admin",
    password: "password123"
};
