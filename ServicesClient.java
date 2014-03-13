package com.be2.services.client;

import com.be2.logging.SystemLogger;
import com.be2.services.rs.*;
import com.be2.utils.JSONUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJacksonProvider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.ProcessingException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("servicesClient")
public class ServicesClient {

    public static final String API_URI_AUTH = "oauth/token";
    public static final String API_URI_USER = "api/user";
    public static final String API_URI_STATIC = "api/staticData";
    public static final String API_URI_EMAIL = "api/email";
    public static final String API_URI_MESSAGE = "api/message";
    public static final String API_URI_MATCHING = "api/matching";
    public static final String API_URI_PHOTO = "api/photo";
    public static final String API_URI_PAYMENT = "api/payment";
    public static final String API_URI_ENCRYPTION = "api/encryption";
    public static final String API_URI_TASK = "api/task";
    public static final String API_URI_REPORT = "api/report";
    public static final String API_URI_EMAIL_TASK_SETTING = "api/ets";
    public static final String API_URI_PRODUCT = "api/product";
    public static final String API_URI_SOCIAL = "api/social";
    public static final String API_URI_DATA_IMPORT_EXPORT = "api/dataImportExport";
    public static final String API_URI_SCAMMER = "api/scammer";

    /**
     */
    @Value("${services.url:http://localhost:8080/services}")
    private String url;

    @Value("${services.clientId:}")
    private String clientId;

    @Value("${services.clientSecret:}")
    private String clientSecret;

    @Value("${services.preAuthorizedToken:}")
    private String preAuthorizedToken;

    @Value("${services.http.maxConnectionCount:100}")
    public int maxConnectionCount;

    private String token;

    private ApacheHttpClient4Executor executor;
    private ResteasyProviderFactory factory;

    private UserResource userResource;
    private StaticResource staticResource;
    private EmailResource emailResource;
    private MessageResource messageResource;
    private MatchingResource matchingResource;
    private PhotoResource photoResource;
    private PaymentResource paymentResource;
    private EncryptionResource encryptionResource;
    private TaskResource taskResource;
    private EmailTaskSettingResource emailTaskSettingResource;
    private ReportResource reportResource;
    private ProductResource productResource;
    private SocialResource socialResource;
    private DataImportExportResource dataImportExportResource;
    private ScammerResource scammerResource;

    // not really a good hack, should be replaced with reentrant lock
    private boolean initialized;

    /**
     *
     * @return productResource
     */
    public ProductResource getProductResource() {
        if(!initialized) {
            initProxies();
        }
        return productResource;
    }

    /**
     * @return
     */
    public PhotoResource getPhotoResource() {
        if(!initialized) {
            initProxies();
        }
        return this.photoResource;
    }

    /**
     * @return
     */
    public UserResource getUserResource() {
        if(!initialized) {
            initProxies();
        }
        return this.userResource;
    }

    /**
     * @return
     */
    public MessageResource getMessageResource() {
        if(!initialized) {
            initProxies();
        }
        return this.messageResource;
    }

    /**
     * @return
     */
    public StaticResource getStaticResource() {
        if(!initialized) {
            initProxies();
        }
        return this.staticResource;
    }

    /**
     * @return
     */
    public EmailResource getEmailResource() {
        if(!initialized) {
            initProxies();
        }
        return this.emailResource;
    }

    /**
     * @return
     */
    public MatchingResource getMatchingResource() {
        if(!initialized) {
            initProxies();
        }
        return this.matchingResource;
    }

    /**
     * @return
     */
    public PaymentResource getPaymentResource() {
        if(!initialized) {
            initProxies();
        }
        return this.paymentResource;
    }

    /**
     * @return
     */
    public EncryptionResource getEncryptionResource() {
        if(!initialized) {
            initProxies();
        }
        return this.encryptionResource;
    }

    /**
     * @return
     */
    public TaskResource getTaskResource() {
        if(!initialized) {
            initProxies();
        }
        return taskResource;
    }

    public EmailTaskSettingResource getEmailTaskSettingResource() {
        if(!initialized) {
            initProxies();
        }
        return emailTaskSettingResource;
    }

    /**
     * Gets reportResource.
     *
     * @return Value of reportResource.
     */
    public ReportResource getReportResource() {
        if(!initialized) {
            initProxies();
        }
        return reportResource;
    }

    /**
     * @return
     */
    public SocialResource getSocialResource() {
        if(!initialized) {
            initProxies();
        }
        return this.socialResource;
    }

    /**
     * Gets DataImportExportResource
     * @return
     */
    public DataImportExportResource getDataImportExportResource() {
        if(!initialized) {
            initProxies();
        }
        return dataImportExportResource;
    }

    /**
     * @return
     */
    public ScammerResource getScammerResource() {
        if(!initialized) {
            initProxies();
        }
        return this.scammerResource;
    }

    private void initConnectionManager(){
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();

        cm.setDefaultMaxPerRoute(maxConnectionCount);
        cm.setMaxTotal(maxConnectionCount);

        DefaultHttpClient httpClient = new DefaultHttpClient(cm);

        httpClient.addRequestInterceptor(new HttpRequestInterceptor(){
            @Override
            public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
                httpRequest.setHeader("Authorization", "Bearer " + token);
            }
        });
        executor = new ApacheHttpClient4Executor(httpClient);

        factory = ResteasyProviderFactory.getInstance();
        factory.register(new ResteasyJacksonProvider());
    }

    /**
     * Creates JAXR proxies for Auth REST resources.
     */
    protected void initProxies() {
        String formattedUrl = this.url.trim();
        initAccessToken();
        initConnectionManager();

        userResource = initResourceProxy(formattedUrl, API_URI_USER, UserResource.class, executor, factory);
        staticResource = initResourceProxy(formattedUrl, API_URI_STATIC, StaticResource.class, executor, factory);
        emailResource = initResourceProxy(formattedUrl, API_URI_EMAIL, EmailResource.class, executor, factory);
        messageResource = initResourceProxy(formattedUrl, API_URI_MESSAGE, MessageResource.class, executor, factory);
        matchingResource = initResourceProxy(formattedUrl, API_URI_MATCHING, MatchingResource.class, executor, factory);
        photoResource = initResourceProxy(formattedUrl, API_URI_PHOTO, PhotoResource.class, executor, factory);
        paymentResource = initResourceProxy(formattedUrl, API_URI_PAYMENT, PaymentResource.class, executor, factory);
        encryptionResource = initResourceProxy(formattedUrl, API_URI_ENCRYPTION, EncryptionResource.class, executor, factory);
        taskResource = initResourceProxy(formattedUrl, API_URI_TASK, TaskResource.class, executor, factory);
        emailTaskSettingResource = initResourceProxy(formattedUrl, API_URI_EMAIL_TASK_SETTING, EmailTaskSettingResource.class, executor, factory);
        reportResource = initResourceProxy(formattedUrl, API_URI_REPORT, ReportResource.class, executor, factory);
        productResource = initResourceProxy(formattedUrl, API_URI_PRODUCT, ProductResource.class, executor, factory);
        socialResource = initResourceProxy(formattedUrl, API_URI_SOCIAL, SocialResource.class, executor, factory);
        dataImportExportResource = initResourceProxy(formattedUrl, API_URI_DATA_IMPORT_EXPORT, DataImportExportResource.class, executor, factory);
        scammerResource = initResourceProxy(formattedUrl, API_URI_SCAMMER, ScammerResource.class, executor, factory);

        initialized = true;
    }

    /**
     *
     *
     * @param url
     * @param path
     * @param clazz
     * @param executor
     * @param factory @return
     */
    private static <T> T initResourceProxy(String url, String path, Class<T> clazz, ApacheHttpClient4Executor executor, ResteasyProviderFactory factory) {
        T t = null;

        try {
             t = ProxyFactory.create(clazz, new URL(url + "/" + path).toURI(), executor, factory);

            AspectJProxyFactory aspectFactory = new AspectJProxyFactory(t);
            aspectFactory.addInterface(clazz);
            aspectFactory.addAspect(ResourceCallInterceptor.class);

            t = (T) aspectFactory.getProxy();
        } catch (URISyntaxException | MalformedURLException e) {
            SystemLogger.error(e);
        }

        return t;
    }

    private void initAccessToken() {
        if (!preAuthorizedToken.isEmpty()) {
            token = preAuthorizedToken;
        } else if (!clientId.isEmpty()) {
            try {
                this.token = getAccessToken(clientId, clientSecret);
            } catch (ProcessingException ex) {
                SystemLogger.error(ex);
            }
        }
    }

    private String getAccessToken(String clientId, String clientSecret) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            URL serviceUrl = new URL(url);
            HttpHost targetHost = new HttpHost(serviceUrl.getHost(), serviceUrl.getPort());
            httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(clientId, clientSecret));

            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);

            BasicHttpContext localcontext = new BasicHttpContext();
            localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);

            HttpPost httpget = new HttpPost(serviceUrl.getPath()+"/"+ API_URI_AUTH);
            List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            nvps.add(new BasicNameValuePair("grant_type", "client_credentials"));
            httpget.setEntity(new UrlEncodedFormEntity(nvps));

            HttpResponse response = httpclient.execute(targetHost, httpget, localcontext);
            HttpEntity entity = response.getEntity();
            String responseStr = EntityUtils.toString(entity);
            Map<String, String> respJSON = (Map<String, String>)JSONUtils.getMapper().readValue(responseStr, Map.class);

            token = respJSON.get("access_token");
        } catch (IOException e) {
            SystemLogger.error(e);
        }finally {
            httpclient.getConnectionManager().shutdown();
        }

        return token;
    }
}
