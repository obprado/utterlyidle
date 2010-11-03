package com.googlecode.utterlyidle;

import com.googlecode.totallylazy.Option;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static com.googlecode.utterlyidle.SeeOther.seeOther;
import static com.googlecode.utterlyidle.RequestBuilder.delete;
import static com.googlecode.utterlyidle.RequestBuilder.get;
import static com.googlecode.utterlyidle.RequestBuilder.post;
import static com.googlecode.utterlyidle.RequestBuilder.put;
import static com.googlecode.utterlyidle.io.Converter.asString;
import static com.googlecode.utterlyidle.proxy.Resource.redirect;
import static com.googlecode.utterlyidle.proxy.Resource.resource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RestTest {
    @Test
    public void canGet() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(Gettable.class);
        assertThat(engine.handle(get("foo")), is("bar"));
    }

    @Test
    public void leadingSlashInPathShouldNotChangeMatch() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(Gettable.class);
        assertThat(engine.handle(get("/foo")), is("bar"));
    }

    @Test
    public void canGetWithQueryParameter() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(GettableWithQuery.class);
        assertThat(engine.handle(get("foo").withQuery("name", "value")), is("value"));
    }

    @Test
    public void canPostWithFormParameter() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(Postable.class);
        assertThat(engine.handle(post("foo").withForm("name", "value")), is("value"));
    }

    @Test
    public void canHandlePathsOnMethodAsWellAsClass() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(MutlilplePaths.class);
        assertThat(engine.handle(get("foo/bar")), is("found"));
    }

    @Test
    public void canDetermineMethodWhenThereIsAChoice() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(MultipleGets.class);
        assertThat(engine.handle(get("foo")), is("no parameters"));
        assertThat(engine.handle(get("foo").withQuery("arg", "match")), is("match"));
    }

    @Test
    public void canDetermineGetMethodBasedOnMimeType() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(GetsWithMimeTypes.class);
        assertThat(engine.handle(get("text").accepting("text/plain")), is("plain"));
        assertThat(engine.handle(get("text").accepting("text/html")), is("html"));
    }

    @Test
    public void setsResponseMimeType() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(GetsWithMimeTypes.class);

        Response response = new Response();
        engine.handle(get("text").accepting("text/plain"), response);
        assertThat(response.headers.getValue(HttpHeaders.CONTENT_TYPE), is("text/plain"));
    }

    @Test
    public void canHandleRealWorldAcceptsHeader() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(GetsWithMimeTypes.class);
        String mimeTypes = "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5";
        assertThat(engine.handle(get("text").accepting(mimeTypes)), is("xml"));

        engine.add(PutContent.class);
        InputStream input = new ByteArrayInputStream("input".getBytes());
        mimeTypes = "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2";
        assertThat(engine.handle(put("path/foo").accepting(mimeTypes).withInput(input)), is("input"));
    }

    @Test
    public void canStreamOutput() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(StreamOutput.class);
        assertThat(engine.handle(get("foo")), is("stream"));
    }

    @Test
    public void canHandleStreamingWriter() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(StreamWriter.class);
        assertThat(engine.handle(get("foo")), is("writer"));
    }

    @Test
    public void supportsNoContent() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(NoContent.class);
        Response response = new Response();
        engine.handle(post("foo"), response);
        assertThat(response.code, is(Status.NO_CONTENT));
    }

    @Test
    public void supportsPathParameter() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(PathParameter.class);
        assertThat(engine.handle(get("path/bar")), is("bar"));
    }

    @Test
    public void supportsDelete() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(DeleteContent.class);
        Response response = new Response();
        engine.handle(delete("path/bar"), response);
        assertThat(response.code, is(Status.NO_CONTENT));
    }

    @Test
    public void supportsPut() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(PutContent.class);

        InputStream input = new ByteArrayInputStream("input".getBytes());
        assertThat(engine.handle(put("path/bar").withInput(input)), is("input"));
    }

    @Test
    public void canDetermineInputHandlerByMimeType() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(MultiplePutContent.class);

        assertThat(engine.handle(put("text").withHeader(HttpHeaders.CONTENT_TYPE, "text/plain")), is("plain"));
        assertThat(engine.handle(put("text").withHeader(HttpHeaders.CONTENT_TYPE, "text/html")), is("html"));
    }

    @Test
    public void canPostRedirectGet() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(PostRedirectGet.class);
        Response response = new Response();
        engine.handle(post("path/bob"), response);
        assertThat(engine.handle(get(response.headers.getValue(HttpHeaders.LOCATION))), is("bob"));
    }

    @Test
    public void canCoerceTypes() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(GetWithStrongType.class);
        assertThat(engine.handle(get("path/4d237b0a-535f-49e9-86ca-10d28aa3e4f8")), is("4d237b0a-535f-49e9-86ca-10d28aa3e4f8"));
    }

    @Test
    public void canCoerceOptionalTypes() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(GetWithOptionalStrongType.class);
        assertThat(engine.handle(get("path").withQuery("id", "4d237b0a-535f-49e9-86ca-10d28aa3e4f8")), is("4d237b0a-535f-49e9-86ca-10d28aa3e4f8"));
    }

    @Test
    public void canCoerceOptionalTypesEvenWhenNoValueIsPresent() throws Exception {
        TestEngine engine = new TestEngine();
        engine.add(GetWithOptionalStrongType.class);
        assertThat(engine.handle(get("path/")), is("default"));
    }

    @Test
    public void supportsCustomRenderer() throws Exception {
        TestEngine engine = new TestEngine();
        engine.addRenderer(MyCustomClass.class, new Renderer<MyCustomClass>() {
            public String render(MyCustomClass value) {
                return "foo";
            }
        });
        engine.add(GetReturningMyCustomClass.class);
        assertThat(engine.handle(get("path")), is("foo"));
    }


    @Path("foo")
    public static class Gettable {
        @GET
        public String get() {
            return "bar";
        }
    }

    @Path("foo")
    public static class GettableWithQuery {
        @GET
        public String get(@QueryParam("name") String name) {
            return name;
        }
    }

    @Path("foo")
    public static class Postable {
        @POST
        public String post(@FormParam("name") String name) {
            return name;
        }
    }

    @Path("foo")
    public static class MutlilplePaths {
        @GET
        @Path("bar")
        public String get() {
            return "found";
        }
    }

    @Path("foo")
    public static class MultipleGets {
        @GET
        public String get() {
            return "no parameters";
        }

        @GET
        public String get(@QueryParam("arg") String arg) {
            return arg;
        }
    }

    @Path("text")
    public static class GetsWithMimeTypes {
        @GET
        @Produces("text/plain")
        public String getPlain() {
            return "plain";
        }

        @GET
        @Produces("application/xml")
        public String getXml() {
            return "xml";
        }

        @GET
        @Produces("text/html")
        public String getHtml() {
            return "html";
        }
    }

    @Path("foo")
    public static class StreamOutput {
        @GET
        public StreamingOutput get() {
            return new StreamingOutput() {
                public void write(OutputStream out) throws IOException {
                    Writer streamWriter = new OutputStreamWriter(out);
                    streamWriter.write("stream");
                    streamWriter.flush();
                }
            };
        }
    }

    @Path("foo")
    public static class StreamWriter {
        @GET
        public StreamingWriter get() {
            return new StreamingWriter() {
                public void write(Writer writer) throws IOException {
                    writer.write("writer");
                }
            };
        }
    }

    @Path("foo")
    public static class NoContent {
        public int count = 0;

        @POST
        public void post() {
            count = count + 1;
        }
    }

    @Path("path/{id}")
    public static class DeleteContent {
        public int count = 0;

        @DELETE
        public void delete(@PathParam("id") String id) {
            count = count + 1;
        }
    }

    @Path("path/{id}")
    public static class PutContent {
        @PUT
        public String put(@PathParam("id") String id, InputStream input) {
            return asString(input);
        }
    }

    @Path("path/{id}")
    public static class PathParameter {
        @GET
        public String get(@PathParam("id") String id) {
            return id;
        }
    }

    @Path("text")
    public static class MultiplePutContent {
        @PUT
        @Consumes("text/plain")
        public String putPlain(InputStream input) {
            return "plain";
        }

        @PUT
        @Consumes("text/html")
        public String putHtml(InputStream input) {
            return "html";
        }
    }

    @Path("path/{id}")
    public static class PostRedirectGet {
        @POST
        public Redirect post(@PathParam("id") String id) {
            return redirect(resource(PostRedirectGet.class).get(id));
        }

        @GET
        public String get(@PathParam("id") String id) {
            return id;
        }
    }

    @Path("path/{id}")
    public static class GetWithStrongType {
        @GET
        public String get(@PathParam("id") Id id) {
            return id.toString();
        }
    }

    @Path("path")
    public static class GetWithOptionalStrongType {
        @GET
        public String get(@QueryParam("id") Option<Id> id) {
            return id.getOrElse(Id.id("default")).toString();
        }
    }

    public static class MyCustomClass {
    }

    @Path("path")
    public static class GetReturningMyCustomClass {
        @GET
        public MyCustomClass get() {
            return new MyCustomClass();
        }
    }
}