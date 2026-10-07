package com.parasoft.parabank.web;

import com.parasoft.parabank.domain.Customer;
import com.parasoft.parabank.domain.logic.BankManager;
import com.parasoft.parabank.util.Constants;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.*;

/** Local customer-API boundary. Core banking methods remain usable by internal callers. */
public final class CustomerBoundaryFilter implements Filter {
    private BankManager bank;
    @Override public void init(FilterConfig config) {
        bank = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext())
                .getBean("bankManager", BankManager.class);
    }
    @Override public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request=(HttpServletRequest)input;
        HttpServletResponse response=(HttpServletResponse)output;
        try {
            String path=java.net.URI.create(request.getRequestURI()).getPath()
                    .substring(request.getContextPath().length()).replaceAll(";[^/]*", "");
            if (path.equals("/services/ParaBank")) {
                if(request.getMethod().equals("GET") && request.getParameter("wsdl")!=null){chain.doFilter(request,response);return;}
                byte[] bytes=request.getInputStream().readNBytes(1048577);
                if (bytes.length>1048576) throw new IllegalArgumentException("Request too large");
                Document doc;
                try {
                    DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                    doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
                }
                catch (Exception e) {throw new IllegalArgumentException("Invalid SOAP request",e);}
                Node body=doc.getElementsByTagNameNS("*","Body").item(0);
                if(body==null)throw new IllegalArgumentException("Missing SOAP body");
                Element operation=null;
                for(Node n=body.getFirstChild();n!=null;n=n.getNextSibling())
                    if(n instanceof Element){if(operation!=null)throw new IllegalArgumentException("Multiple operations");operation=(Element)n;}
                if(operation==null)throw new IllegalArgumentException("Missing SOAP operation");
                String name=operation.getLocalName();
                if(!publicOperation(name)) {
                    int owner=identity(request);
                    Map<String,String> fields=new HashMap<>();
                    for(Node n=operation.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element){
                        if(fields.put(n.getLocalName(),n.getTextContent())!=null)throw new IllegalArgumentException("Duplicate SOAP parameter");
                    }
                    authorize(owner,fields);
                    validateExternalAmount(name,fields.get("amount"));
                }
                request=new BodyRequest(request,bytes);
            } else if(path.startsWith("/services/bank") || path.startsWith("/services_proxy/bank")) {
                String route=path.replaceFirst("^/services(?:_proxy)?/bank/?", "");
                String operation=route.split("/",2)[0];
                boolean metadata=route.isEmpty() && (request.getParameter("_wadl")!=null || request.getParameter("_openapi")!=null);
                if(!metadata && !publicOperation(operation)) {
                    int owner=identity(request);
                    Map<String,String> fields=new HashMap<>();
                    for(String key:List.of("customerId","accountId","fromAccountId","transactionId","amount")) {
                        String[] values=request.getParameterValues(key);
                        if(values!=null){if(values.length!=1)throw new IllegalArgumentException("Duplicate parameter");fields.put(key,values[0]);}
                    }
                    Matcher account=Pattern.compile("^accounts/([^/]+)(?:/.*)?$").matcher(route);
                    if(account.matches())requireAccount(owner,integer(account.group(1)));
                    Matcher customer=Pattern.compile("^customers/(?:update/)?([^/]+)(?:/.*)?$").matcher(route);
                    if(customer.matches())requireCustomer(owner,integer(customer.group(1)));
                    Matcher transaction=Pattern.compile("^transactions/([^/]+)$").matcher(route);
                    if(transaction.matches())requireAccount(owner,bank.getTransaction(integer(transaction.group(1))).getAccountId());
                    authorize(owner,fields);
                    validateExternalAmount(operation,fields.get("amount"));
                }
            }
            chain.doFilter(request,response);
        } catch (AccessDenied e) {error(response,e.status,e.getMessage());}
        catch (IllegalArgumentException e) {error(response,400,e.getMessage());}
        catch (ServletException e) {
            Throwable cause=e;
            while(cause!=null && !(cause instanceof IllegalArgumentException))cause=cause.getCause();
            if(cause!=null)error(response,400,cause.getMessage());else throw e;
        }
    }
    private boolean publicOperation(String name) {
        // These administration endpoints are retained for the loopback-only assignment environment.
        return Set.of("login","initializeDB","cleanDB","setParameter","getParameter","getParameters").contains(name);
    }
    private int identity(HttpServletRequest request) {
        String auth=request.getHeader("Authorization");
        if(auth!=null) {
            if(!auth.regionMatches(true,0,"Basic ",0,6))throw new AccessDenied(401,"Authentication required");
            try {
                String credentials=new String(Base64.getDecoder().decode(auth.substring(6).trim()),StandardCharsets.UTF_8);
                int colon=credentials.indexOf(':');
                if(colon<1)throw new IllegalArgumentException();
                Customer c=bank.getCustomer(credentials.substring(0,colon),credentials.substring(colon+1));
                if(c==null)throw new IllegalArgumentException();
                return c.getId();
            }catch(RuntimeException e){throw new AccessDenied(401,"Authentication required");}
        }
        HttpSession session=request.getSession(false);
        Object value=session==null?null:session.getAttribute(Constants.USERSESSION);
        if(!(value instanceof UserSession))throw new AccessDenied(401,"Authentication required");
        return ((UserSession)value).getCustomer().getId();
    }
    private void authorize(int owner,Map<String,String> fields) {
        for(String key:List.of("accountId","fromAccountId"))if(fields.containsKey(key))requireAccount(owner,integer(fields.get(key)));
        if(fields.containsKey("customerId"))requireCustomer(owner,integer(fields.get("customerId")));
        if(fields.containsKey("transactionId"))requireAccount(owner,bank.getTransaction(integer(fields.get("transactionId"))).getAccountId());
    }
    private int integer(String value) {
        try{return Integer.parseInt(value);}catch(RuntimeException e){throw new IllegalArgumentException("Invalid identifier");}
    }
    private void requireAccount(int owner,int account) {
        if(bank.getAccount(account).getCustomerId()!=owner)throw new AccessDenied(403,"Account access denied");
    }
    private void requireCustomer(int owner,int customer) {
        if(owner!=customer)throw new AccessDenied(403,"Customer access denied");
    }
    private void validateExternalAmount(String operation,String value) {
        if(!Set.of("deposit","withdraw","transfer","billpay").contains(operation))return;
        try {
            BigDecimal amount=new BigDecimal(value);
            if(amount.signum()<=0 || amount.stripTrailingZeros().scale()>2 || amount.precision()>17)
                throw new IllegalArgumentException();
        }catch(RuntimeException e){throw new IllegalArgumentException("Amount must be positive with at most two decimal places");}
    }
    private void error(HttpServletResponse response,int status,String message) throws IOException {
        if(response.isCommitted())throw new IOException("Cannot replace committed error response");
        response.reset();response.setStatus(status);response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message==null?"Invalid request":message);
    }
    private static final class AccessDenied extends RuntimeException {
        final int status;AccessDenied(int status,String message){super(message);this.status=status;}
    }
    private static final class BodyRequest extends HttpServletRequestWrapper {
        private final byte[] bytes;BodyRequest(HttpServletRequest request,byte[] bytes){super(request);this.bytes=bytes;}
        @Override public ServletInputStream getInputStream(){
            ByteArrayInputStream input=new ByteArrayInputStream(bytes);
            return new ServletInputStream(){public int read(){return input.read();}public boolean isFinished(){return input.available()==0;}public boolean isReady(){return true;}public void setReadListener(ReadListener listener){throw new UnsupportedOperationException("Blocking SOAP transport");}};
        }
        @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
    }
}
