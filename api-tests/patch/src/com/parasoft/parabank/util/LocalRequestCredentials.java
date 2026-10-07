package com.parasoft.parabank.util;
import java.net.*;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.BindingProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Propagate caller identity only for the same application's local optional access modes. */
public final class LocalRequestCredentials {
    private static Map<String,List<String>> headers(String endpoint) {
        if(!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes))return Map.of();
        HttpServletRequest request=attributes.getRequest();
        try {
            URI uri=URI.create(endpoint);
            int port=uri.getPort()<0?("https".equals(uri.getScheme())?443:80):uri.getPort();
            String host=uri.getHost();
            boolean local=Set.of("localhost","127.0.0.1","[::1]","::1").contains(host);
            if(!local || port!=request.getLocalPort() || !uri.getPath().startsWith(request.getContextPath()+"/services/"))return Map.of();
            Map<String,List<String>> result=new HashMap<>();
            for(String name:List.of("Cookie","Authorization")) {
                String value=request.getHeader(name);if(value!=null)result.put(name,List.of(value));
            }
            return result;
        }catch(RuntimeException e){return Map.of();}
    }
    public static void propagate(HttpURLConnection connection) {
        headers(connection.getURL().toString()).forEach((name,values)->connection.setRequestProperty(name,values.get(0)));
    }
    public static void propagate(Object service) {
        if(service instanceof BindingProvider provider) {
            Object endpoint=provider.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            if(endpoint!=null) {
                Map<String,List<String>> values=headers(endpoint.toString());
                if(!values.isEmpty())provider.getRequestContext().put("jakarta.xml.ws.http.request.headers",values);
            }
        }
    }
}
