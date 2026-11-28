package org.bitmagic.ifeed.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.bitmagic.ifeed.security.CredentialType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;

@RestController
public class McpController {
    public static final String PATH = "/api/mcp";
    private final HttpServletStatelessServerTransport transport;
    private final McpStatelessSyncServer server;

    public McpController(ObjectMapper json, McpTools tools) {
        var mapper = new JacksonMcpJsonMapper(json);
        transport = HttpServletStatelessServerTransport.builder().messageEndpoint(PATH).jsonMapper(mapper).maxRequestSize(1024 * 1024).build();
        server = McpServer.sync(transport).serverInfo("ifeed", "1.0.0").jsonMapper(mapper)
                .jsonSchemaValidator(new DefaultJsonSchemaValidator(json))
                .capabilities(ServerCapabilities.builder().tools(false).build()).immediateExecution(true)
                .addToolFilter((context, tool) -> tools.visible(tool.name())).tools(tools.specifications()).build();
    }

    @RequestMapping(value = PATH, method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.DELETE})
    public void handle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!CredentialType.MCP.matches(authentication))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "需要有效的 MCP 访问凭证");
        String version = request.getHeader("MCP-Protocol-Version");
        if (version != null && !transport.protocolVersions().contains(version))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的 MCP 协议版本");
        String origin = request.getHeader("Origin");
        if (origin != null) {
            var target = URI.create(request.getRequestURL().toString());
            if (!(target.getScheme() + "://" + target.getRawAuthority()).equalsIgnoreCase(origin))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不允许该来源访问 MCP");
        }
        transport.service(request, response);
    }

    @PreDestroy
    public void close() { server.closeGracefully(); }
}
