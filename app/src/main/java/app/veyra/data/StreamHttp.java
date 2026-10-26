package app.veyra.data;
import app.veyra.model.StreamSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class StreamHttp {
    private StreamHttp() {}
    public static DataSource.Factory factory(StreamSource source) {
        HttpUrl origin=HttpUrl.get(source.url);
        OkHttpClient client=new OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS)
            .followSslRedirects(false).addNetworkInterceptor(chain->{
                Request original=chain.request();HttpUrl target=original.url();Request.Builder request=original.newBuilder();
                if(!target.isHttps() && !(target.host().equals("127.0.0.1") && target.port()==origin.port() && CatalogBackend.isActiveProxy(source.url)))
                    throw new IOException("Insecure stream transport was rejected");
                // Signed headers belong only to the originating server, including after redirects.
                if(!target.scheme().equals(origin.scheme()) || !target.host().equals(origin.host()) || target.port()!=origin.port())
                    for(String name:source.headers.keySet())request.removeHeader(name);
                return chain.proceed(request.build());
            }).build();
        return new OkHttpDataSource.Factory(client).setDefaultRequestProperties(source.headers);
    }
}
